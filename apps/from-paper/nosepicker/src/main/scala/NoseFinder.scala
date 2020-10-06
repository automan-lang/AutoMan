import java.io.File

import org.automanlang.adapters.mturk._
import org.automanlang.core.answer._
import com.github.tototoshi.csv._
import org.automanlang.automan
import org.automanlang.core.logging.LogLevelDebug
import org.automanlang.core.policy.aggregation.UserDefinableSpawnPolicy
import org.automanlang.core.question.Dim
import org.automanlang.core.question.confidence.SymmetricCI

import scala.io.Source
import scala.util.Random

object NoseFinder extends App {
  // parse options
  val opts = new Conf(args)

  // emit hash
  //println(s"Nose Finder benchmark ver. ${currentHash()} (AutoMan ver. ${org.automanlang.core.util.GitHash.value}), starting up...")

  // init random
  val r = new Random()

  // open files
  val images = r.shuffle(Source.fromFile(opts.images()).getLines).take(opts.numImages()).toArray
  val output = new File(opts.output())
  val output_csv = CSVWriter.open(output)
  output_csv.writeRow(List("gitversion", "image", "question_id", "success", "xestimate", "xlow", "xhigh", "yestimate", "ylow", "yhigh", "cost", "conf", "need", "have"))

  // init AutoMan
  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts.key()
    mt.secret_access_key = opts.secret()
    mt.sandbox_mode = opts.sandbox()
    mt.log_verbosity = LogLevelDebug()
    mt.database_path = opts.database_path.get match {
      case Some(dbpath) => dbpath
      case None => "NoseFinder"
    }
  }

  val layout = scala.xml.Unparsed(
    """
      |<script type='text/javascript' src="https://ajax.googleapis.com/ajax/libs/jquery/2.2.0/jquery.min.js"></script>
      |<script type='text/javascript'>
      |  function drawImage(x,y,drawit) {
      |    var c = document.getElementById('myCanvas');
      |    var ctx = c.getContext('2d');
      |    ctx.clearRect(0, 0, 500, 400);
      |    var img = new Image();
      |    img.onload = function() {
      |        ctx.drawImage(img, 0, 0);
      |        if (drawit) {
      |          ctx.beginPath();
      |          ctx.lineWidth="5";
      |          ctx.strokeStyle="red";
      |          ctx.moveTo(x-20,y);
      |          ctx.lineTo(x+20,y);
      |          ctx.moveTo(x,y-20);
      |          ctx.lineTo(x,y+20);
      |          ctx.stroke();
      |
      |          if (!previewMode()) {
      |            $("#submitButton").prop("disabled", false);
      |          }
      |        }
      |    };
      |    img.src = $("#question_image").attr("src");
      |  }
      |
      |  $(function() {
      |    $("#submitButton").prop("disabled", true);
      |    $("#wrapper").append("<div id='canvasdiv'><canvas id='myCanvas' width='500' height='400'>Your browser does not support the HTML5 canvas tag.</canvas></div>");
      |    $("#myCanvas").click(function(e) {
      |      var offset = $(this).offset();
      |      var relativeX = (e.pageX - offset.left);
      |      var relativeY = (e.pageY - offset.top);
      |      $("#dimension_x").val(relativeX);
      |      $("#dimension_y").val(relativeY);
      |      drawImage(relativeX, relativeY, true);
      |    });
      |
      |    drawImage(0,0,false);
      |  });
      |</script>
      |<style>
      |  input.dimension { display: none; }
      |  #question_image { display: none; }
      |  #submitButton {
      |    width: 200px;
      |  }
      |</style>
  """.stripMargin
  )

  // define AutoMan function
  def whereIsTheNose(imgUrl: String) = a.MultiEstimationQuestion { q =>
    q.budget = 6.25
    q.title = "Where is this person's nose?"
    q.text = "Locate the center of the tip of this person's nose, " +
      "click on it with your mouse, and press " +
      "the submit button."
    q.image_url = imgUrl
    q.dimensions = Array(
      Dim(id = 'x, confidence_interval = SymmetricCI(20)),
      Dim(id = 'y, confidence_interval = SymmetricCI(20))
    )
    q.layout = layout
  }

  // run program
  automan(a) {
    // schedule all futures
    val estimates: Array[MultiEstimationOutcome] = images.map(whereIsTheNose)

    // now block on responses
    estimates.zipWithIndex.foreach { case (e,i) =>
      e.answer match {
        case MultiEstimate(ests, lows, highs, cost, conf, question, id) =>
          println("Question ID: " + id +
            ", Image: " + images(i) +
            ", x-estimate: " + ests(0) +
            ", x-low: " + lows(0) +
            ", x-high: " + highs(0) +
            ", y-estimate: " + ests(1) +
            ", y-low: " + lows(1) +
            ", y-high: " + highs(1) +
            ", cost: $" + cost +
            ", confidence: " + conf)
          //output_csv.writeRow(List(currentHash(), images(i), id, "estimated", ests(0), lows(0), highs(0), ests(1), lows(1), highs(1), cost, conf, "NA", "NA"))
        case LowConfidenceMultiEstimate(ests, lows, highs, cost, conf, question, id) =>
          println("Question ID: " + id +
            ", Image: " + images(i) +
            ", Low-confidence x-estimate: " + ests(0) +
            ", x-low: " + lows(0) +
            ", x-high: " + highs(0) +
            ", Low-confidence y-estimate: " + ests(1) +
            ", y-low: " + lows(1) +
            ", y-high: " + highs(1) +
            ", cost: $" + cost +
            ", confidence: " + conf)
          //output_csv.writeRow(List(currentHash(), images(i), id, "lowconf", ests(0), lows(0), highs(0), ests(1), lows(1), highs(1), cost, conf, "NA", "NA"))
        case OverBudgetMultiEstimate(need, have, id) =>
          println("Question ID: " + id +
            ", Over budget; could not produce an estimate. Need $" +
            need +"; have $" + have)
          //output_csv.writeRow(List(currentHash(), images(i), id, "overbudget", "NA", "NA", "NA", "NA", "NA", need, have))
      }
    }
  }
}

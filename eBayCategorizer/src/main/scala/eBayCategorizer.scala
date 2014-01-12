package main.scala

import edu.umass.cs.automan.adapters.MTurk.MTurkAdapter
import edu.umass.cs.automan.core.Utilities
import edu.umass.cs.automan.adapters.MTurk.question.MTQuestionOption
import com.ebay.sdk._
import com.ebay.sdk.call._
import com.ebay.soap.eBLBaseComponents.{DetailLevelCodeType, SiteCodeType, CategoryType}
import scala.collection.mutable

object eBayCategorizer extends App {
  val opts = my_optparse(args, "eBayCategorizer")
  val ebay_soap = "https://api.ebay.com/wsapi"

  // init AutoMan for MTurk
  val a = MTurkAdapter { mt =>
    mt.access_key_id = opts('key)
    mt.secret_access_key = opts('secret)
    mt.sandbox_mode = opts('sandbox).toBoolean
  }

  // query eBay for product taxonomy
  val root_categories = GetAllEBayCategories()

  // get product images from disk
  val files = new java.io.File(opts('imagedir)).listFiles

  // upload to S3
  val image_urls = files.map { file => UploadImageToS3(file) }

  // array that says whether categorization is done
  val catdone = Array.fill[Boolean](files.size)(false)

  // array that stores taxonomies as a list
  // the head of the list is the LAST CATEGORY FOUND
  val product_taxonomies = Array.fill[List[CatNode]](files.size)(List.empty)

  // array that stores all of the next choices
  // initialized with the product root categories
  val taxonomy_choices = Array.fill[Set[CatNode]](files.size)(root_categories)

  while(!AllDone(catdone)) {
    // make a map from answer symbols back to CatNodes
    val answer_key = mutable.Map[Symbol,CatNode]()

    // insert each taxonomy choice into the answer key
    taxonomy_choices.foreach { catset => catset.foreach { cat => answer_key += (new Symbol(cat.name) -> cat) } }

    // for each not-done product, launch a classification
    // task using the taxonomy_choices & block until done;
    // for done tasks, just propagate the leaf symbol from
    // the previous step
    val results: Array[Symbol] = (0 until files.size).par.map { i =>
      if (!catdone(i)) {
        // Run the classifier
        // the "()" and ".value" extract the value from the Future and RadioButtonAnswer respectively
        Classify(image_urls(i), GetOptions(taxonomy_choices(i)))().value
      } else {
        new Symbol(product_taxonomies(i).head.id)
      }
    }.toArray

    // update product taxonomies
    (0 until files.size).foreach { i => if (!catdone(i)) product_taxonomies(i) = answer_key(results(i)) :: product_taxonomies(i) }

    // update catdone
    (0 until files.size).foreach { i => catdone(i) = answer_key(results(i)).is_leaf }

    // get the next set of choices
    (0 until files.size).foreach { i => if (!catdone(i)) taxonomy_choices(i) = answer_key(results(i)).Children }
  }

  println("Done.")

  def UploadImageToS3(image_file: java.io.File) : String = {
    // TODO: this is just a stub for now
    image_file.toString
  }

  def GetOptions(choices: Set[CatNode]) : List[MTQuestionOption] = {
    choices.toList.map { node => new MTQuestionOption(new Symbol(node.id), node.name, "") }
  }

  def AllDone(completion_arr: Array[Boolean]) = completion_arr.foldLeft(true)((acc, tval) => acc && tval)

  def Classify(image_url: String, options: List[MTQuestionOption]) = a.RadioButtonQuestion { q =>
    q.title = "Please choose the appropriate category for this image"
    q.text = "Please choose the appropriate category for this image"
    q.image_url = image_url
    q.options = a.Option('none, "None of these categories apply.") :: options
  }

  private def my_optparse(args: Array[String], invoked_as_name: String) : Utilities.OptionMap = {
    val usage = "Usage: " + invoked_as_name + " -k [AWS key] -s [AWS secret] -e [eBay key] -i [image directory] [--sandbox [true | false]]" +
      "\n  NOTE: passing credentials this way will expose" +
      "\n  them to users on this system."
    if (args.length != 8 && args.length != 10) {
      println("You only supplied " + args.length + " arguments.")
      println(usage)
      sys.exit(1)
    }
    val arglist = args.toList
    val opts = nextOption(Map(),arglist)
    if(!opts.contains('sandbox)) {
      opts ++ Map('sandbox -> true.toString)
    } else {
      opts
    }
  }

  private def nextOption(map : Utilities.OptionMap, list: List[String]) : Utilities.OptionMap = {
    list match {
      case Nil => map
      case "-k" :: value :: tail => nextOption(map ++ Map('key -> value), tail)
      case "-s" :: value :: tail => nextOption(map ++ Map('secret -> value), tail)
      case "-e" :: value :: tail => nextOption(map ++ Map('ebay_key -> value), tail)
      case "-i" :: value :: tail => nextOption(map ++ Map('imagedir -> value), tail)
      case "--sandbox" :: value :: tail => nextOption(map ++ Map('sandbox -> value), tail)
      case option :: tail => println("Unknown option "+option)
        sys.exit(1)
    }
  }

  private def initEBay() : ApiContext = {
    val apiContext = new ApiContext()
    apiContext.getApiCredential.seteBayToken(opts('ebay_key))
    apiContext.setApiServerUrl(ebay_soap)
    apiContext
  }

  def GetAllEBayCategories() : Set[CatNode] = {
    class CategoryHandler extends CategoryEventListener {
      private val _cats = new mutable.HashSet[CategoryType]()
      private val _catids = mutable.Map[String,CategoryType]()
      private val _parent_children_map = mutable.Map[String,Set[String]]()
      private val _forest = mutable.Map[String,CatNode]()
      private val _roots = mutable.Set[CatNode]()
      def receivedCategories(siteID: SiteCodeType, categories: Array[CategoryType], categoryVersion: String): Unit = {
        for(category <- categories) {
          val id = category.getCategoryID
          _cats.add(category)
          _catids += (id -> category)
          val parents = category.getCategoryParentID
          for(parent_id <- parents) {
            if (_parent_children_map.contains(parent_id)) {
              val siblings = _parent_children_map(parent_id)
              if (siblings.contains(parent_id)) {
                throw new Exception("Parent is sibling of child! This id = " + category.getCategoryID + ", parents: " + parents.mkString(", "))
              }
              _parent_children_map += (parent_id -> siblings.union(Set(id)))
            } else {
              // idiotically, top-level parents are parents of themselves (have self-loops)
              // so don't propigate the foolishness
              if (parent_id != id) {
                _parent_children_map += (parent_id -> Set(id))
              }
            }
          }
        }
      }

      def Roots = _roots

      def Categories = _cats

      private def BuildCatForest : Unit = {
        for(cat <- _cats) {
          Add(cat)
        }
      }

      private def Add(cat: CategoryType) : CatNode = {
        val id = cat.getCategoryID
        val parents = mutable.Set[CatNode]()
        var cn_opt : Option[CatNode] = None

        if (!_forest.contains(id)) {
          // is this category a root?
          if (cat.getCategoryLevel == 1) {
            // add to _roots
            val cn = CatNode(id, cat.getCategoryName, false, Set.empty)
            _roots += cn
            cn_opt = Some(cn)
          // if not, add parents
          } else {
            // populate parents set with CatNodes for parent nodes
            for (parent <- cat.getCategoryParentID) {
              parents.add(Add(_catids(parent)))
            }

            cn_opt = Some(CatNode(id, cat.getCategoryName, cat.isLeafCategory, parents.toSet))
          }
        }

        cn_opt match {
          // if a CatNode was constructed above, add to forest
          case Some(cn) =>
            _forest += (id -> cn)
            cn
          // otherwise, just retrieve from data structure
          case None => _forest(id)
        }
      }

      private def AddChildren(roots: Set[CatNode]) : Unit = {
        if (!roots.isEmpty) {
          for(root <- roots) {
            // get children ids for root id
            val children_ids = if (_parent_children_map.contains(root.id)) {
              _parent_children_map(root.id)
            } else {
              Set.empty
            }

            val children = mutable.Set[CatNode]()
            for(child_id <- children_ids) {
              if (child_id == root.id) {
                throw new Exception("Child (" + child_id + ") is the same as parent (" + root.id + ")")
              }
              val child = _forest(child_id)
              root.addChild(child)
              children.add(child)
            }

            // recurse
            AddChildren(children.toSet)
          }
        }
      }

      // this method counts the number of nodes in the taxonomy
      private def SanityCheck(roots: Set[CatNode]) : Int = {
        val count = roots.size
        count + roots.foldLeft(0) { (sum, node) => sum + SanityCheck(node.Children) }
      }

      def GetGraph : Set[CatNode] = {
        BuildCatForest
        AddChildren(_roots.toSet)

        // make sure that the number of nodes in the graph
        // is the same as the number of categories in the
        // original XML
        val count = SanityCheck(_roots.toSet)
        if (count != _forest.size) {
          throw new Exception("The in-memory taxonomy size of " + count + " nodes is not the same as the XML data of " + _forest.size + " nodes!")
        }

        _roots.toSet
      }
    }

    // init ebay
    val ebay = initEBay()

    // get categories
    val catobj = new CategoryHandler()
    GetCategoriesCall.getAllCategories(ebay, SiteCodeType.US, 1, DetailLevelCodeType.RETURN_ALL, 100, catobj)

    // return graph
    catobj.GetGraph
  }

  case class CatNode(id: String, name: String, is_leaf: Boolean, parent: Set[CatNode]) {
    val _children = mutable.Set[CatNode]()
    def addChild(child: CatNode) = _children.add(child)
    def Children = _children.toSet
  }
}
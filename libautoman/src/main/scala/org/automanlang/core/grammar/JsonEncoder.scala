package org.automanlang.core.grammar

import io.circe.{Encoder, Json, KeyEncoder}
import io.circe.syntax._  // for asJson implicit conversion

object JsonEncoder {
  implicit val NameKeyEncoder: KeyEncoder[Name] = (n: Name) => n.fullname()

  implicit val encodeName: Encoder[Name] =
    Encoder.forProduct3("text", "depth", "fullname")(n =>
      (n.text, n.depth.getOrElse(-1), n.fullname())
    )

  implicit val encodeExpression: Encoder[Expression] = {
    case expression: TextExpression => expression.asJson
    case OptionProduction(text) => Json.obj(
      ("type", Json.fromString("OptionProduction")),
      ("text", text.asJson)
    )
    case _ => Json.obj(
      ("type", Json.fromString("Unknown")),
      ("warning", Json.fromString("Should not appear inside grammar tree"))
    )
  }

  implicit val encodeTestExpression: Encoder[TextExpression] = {
    case Ref(nt) => Json.obj(
      ("type", Json.fromString("Ref")),
      ("nt", nt.asJson)
    )
    case Binding(nt) => Json.obj(
      ("type", Json.fromString("Binding")),
      ("nt", nt.asJson)
    )
    case Terminal(value) => Json.obj(
      ("type", Json.fromString("Terminal")),
      ("value", value.asJson)
    )
    case Choice(choices) => Json.obj(
      ("type", Json.fromString("Choice")),
      ("choices", choices.zipWithIndex.map{ case (e, i) =>
        e.asJson.deepMerge(Map("index"->i).asJson)
      }.asJson)
    )
    case Sequence(sentence) => Json.obj(
      ("type", Json.fromString("Sequence")),
      ("sentence", sentence.asJson)
    )
    case Function(fun, param, capitalize) => Json.obj(
      ("type", Json.fromString("Function")),
      ("fun", fun.asJson),
      ("param", param.asJson),
      ("capitalize", capitalize.asJson),
    )
    case _ => Json.obj(
      ("type", Json.fromString("Unknown")),
      ("warning", Json.fromString("Should not appear inside grammar tree"))
    )
  }
}

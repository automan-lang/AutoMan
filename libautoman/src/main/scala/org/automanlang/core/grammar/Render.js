/**
 * I test this file in node v16. It should function the same across browsers...
 */

// Alternatively (for our Scala code), we can use string templates
// const grammarText = String.raw`
// <JSON content>
// `
const fs = require("fs");
const grammarText = fs.readFileSync("lindaGrammar_expanded.json");

/**
 * 
 * @param {any} expr 
 * @param {any} g 
 * @param {Array<Number>} assignment 
 * @param {Map<Number, any>} generatingScope 
 * @param {Set<String>} generatedNames 
 * @returns {{soFarScope: Map<Number, any>, soFarNames: Set<String>}}
 */
const bindHelper = (expr, g, assignment, generatingScope, generatedNames) => {
  let index = generatingScope.size;
  let soFarScope = generatingScope;
  let soFarNames = generatedNames;

  // console.log(expr);
  switch (expr["type"]) {
    case "Ref":
      return bindHelper(g[expr["nt"]["fullname"]], g, assignment, generatingScope, new Set(soFarNames).add(expr["nt"]["text"]));
    case "OptionProduction":
      return bindHelper(expr["text"], g, assignment, generatingScope, soFarNames);
    case "Terminal":
    case "Function":
      return {soFarScope, soFarNames};
    case "Sequence":
      expr["sentence"].forEach(e => {
        ({soFarScope, soFarNames} = bindHelper(e, g, assignment, soFarScope, soFarNames));
      });
      return {soFarScope, soFarNames};
    case "Choice":
      let choices = expr["choices"];
      soFarScope.set(index, choices[assignment[index]]);
      choices.forEach(e => {
        ({soFarScope, soFarNames} = bindHelper(e, g, assignment, soFarScope, soFarNames))
      })
      return {soFarScope, soFarNames};
    case "Binding":
      if(!soFarNames.has(expr["nt"]["text"])) {
        return bindHelper(g[expr["nt"]["fullname"]], g, assignment, generatingScope, new Set(soFarNames).add(expr["nt"]["text"]))
      } else {
        return {soFarScope, soFarNames}
      }
    default:
      break;
  }
}

const bind = (g, assignment) => {
  let {soFarScope, } = bindHelper(g["Start"], g, assignment, new Map(), new Set());
  return soFarScope
}



grammar = JSON.parse(grammarText)
// console.log(grammar['A']['sentence'])

// bases = [4, 5, 6, 5, 5, 5, 5]
let assignment = [1, 1, 3, 0, 0, 0, 0]  // variation = 24375

let scope = bind(grammar, assignment);
console.log(scope);

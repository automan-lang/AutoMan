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

/**
 *
 * @param {any} expr
 * @param {any} g
 * @param {Map<Number, any>} scope
 * @param {Array<any>} generatingBod
 * @param {Array<Array<any>>} generatingOpts
 * @param {boolean} doAppend
 * @param {number} index
 * @param {Map<String, String>} boundVars
 * @returns {{bodSoFar: Array<any>, optsSoFar: Array<Array<any>>, boundVarsSoFar: Map<String, String>, position: number}}
 */
const firstRender = (expr, g, scope, generatingBod, generatingOpts, doAppend, index, boundVars) => {
  // TODO: we should be able to remove these variables
  let bodSoFar = generatingBod
  let optsSoFar = generatingOpts
  let boundVarsSoFar = boundVars
  let position = index

  switch (expr["type"]) {
    case "Ref":
      return firstRender(g[expr["nt"]["fullname"]], g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar)
    case "Terminal":
    case "Function":
      if (doAppend) { bodSoFar = bodSoFar.concat(expr); }  // append if we're on the right branch
      return {bodSoFar, optsSoFar, boundVarsSoFar, position};
    case "Sequence":
      expr["sentence"].forEach(e => {
        ({bodSoFar, optsSoFar, boundVarsSoFar, position} = firstRender(e, g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar));
      });
      return {bodSoFar, optsSoFar, boundVarsSoFar, position};
    case "Choice":
      position += 1;
      let choices = expr["choices"];
      choices.forEach(e => {
        let doApp = false;
        // TODO: assuming index comparison works (it will be our simplified version of hashing)
        if (doAppend && scope.get(position)["index"] === e["index"]) {
          doApp = true;
        }
        ({bodSoFar, optsSoFar, boundVarsSoFar, position} = firstRender(e, g, scope, bodSoFar, optsSoFar, doApp, position, boundVarsSoFar));
      })
      return {bodSoFar, optsSoFar, boundVarsSoFar, position};
    case "OptionProduction":
      // here we reuse firstRender instead of reimplementing <ANONYMIZED>'s renderHelper
      // the core idea is to treat the options as body, and reuse the function logic to construct a body
      let {bodSoFar: newOpt, boundVarsSoFar: newBound, position: newPos } = firstRender(expr["text"], g, scope, [], [], doAppend, position, boundVarsSoFar);
      return {bodSoFar, optsSoFar: optsSoFar.concat([newOpt]), boundVarsSoFar: newBound, position: newPos};
    case "Binding":
      let startArr = Array.from(bodSoFar);

      if(!boundVarsSoFar.has(expr["nt"]["text"])) {
        ({bodSoFar, optsSoFar, boundVarsSoFar, position} = firstRender(g[expr["nt"]["fullname"]], g, scope, bodSoFar, optsSoFar, doAppend, position, boundVarsSoFar))
        let added = bodSoFar.filter(x => !startArr.includes(x));  // difference: figure out what was added

        let toAdd = added.map(x => x["value"]).join("");
        boundVarsSoFar = new Map(boundVarsSoFar).set(expr["nt"]["text"], toAdd);
      } else {  // if have seen, look up binding and add to instance
        bodSoFar = bodSoFar.concat(JSON.parse(`{"type" : "Terminal","value" : "${boundVarsSoFar.get(expr["nt"]["text"])}"}`))
      }
      return {bodSoFar, optsSoFar, boundVarsSoFar, position};
    default:
      console.log("ERROR: UNKNOWN TYPE");
      break;
  }
}

/**
 * Helper method to generate a string from the Expression arrays by appending Terminals and calling Functions
 * @param {Array<any>} instArr
 * @param {Map<String, String>} bingdingMap
 * @returns {String}
 */
const secondRenderHelper = (instArr, bingdingMap) => {
  let instance = [];
  instArr.forEach(e => {
    switch (e["type"]) {
      case "Terminal":
        instance.push(e["value"]);
        break;
      case "Function":
        let text = e["fun"][bingdingMap.get(e["param"]["text"])];
        if (e["capitalize"]) {
          text = text.charAt(0).toUpperCase() + text.slice(1)
        }
        instance.push(text);
        break;
      default:
        break;
    }
  })
  return instance.join("");
}

const secondRender = (bodArr, optsArr, bingdingMap) => {
  return {
    bod: secondRenderHelper(bodArr, bingdingMap),
    opts: optsArr.map(o => secondRenderHelper(o, bingdingMap))
  }
}

/**
 * Creates a string of the experiment instance specified by the Scope.
 *
 * @param {Map<Number, any>} scope
 * @param {any} grammar
 * @returns
 */
const renderInstance = (scope, grammar) => {
  let {bodSoFar, optsSoFar, boundVarsSoFar} = firstRender(grammar["Start"], grammar, scope, Array(), Array(), true, -1, new Map())
  // By now, all grammar should be left with only terminals and functions, we should secondRender to concatenate them and apply functions
  return secondRender(bodSoFar, optsSoFar, boundVarsSoFar);
}

grammar = JSON.parse(grammarText)
// console.log(grammar['A']['sentence'])

// bases = [4, 5, 6, 5, 5, 5, 5]
let assignment = [1, 1, 3, 0, 0, 0, 0]  // variation = 24375

/*
Map(7) {
  0 => { type: 'Terminal', value: 'Dan' },
  1 => { type: 'Terminal', value: '31' },
  2 => { type: 'Terminal', value: 'philosophy' },
  3 => { type: 'Terminal', value: 'discrimination and social justice' },
  4 => { type: 'Terminal', value: 'anti-nuclear' },
  5 => { type: 'Terminal', value: 'bank teller' },
  6 => { type: 'Terminal', value: 'feminist' }
}
*/
let scope = bind(grammar, assignment);
console.log("scope:", scope);

let {bod, opts} = renderInstance(scope, grammar);
console.log("body:", bod)
console.log("options:", opts)

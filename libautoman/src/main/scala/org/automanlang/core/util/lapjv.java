/**
 * The following codes in this file are taken from project
 * <a href="https://github.com/dberm22/Bipartite-Solver/tree/dabaf0f6fd60a4c91b27960c119ccd2435968041">dberm22/Bipartite-Solver</a>
 * on GitHub at commit <code>dabaf0f6fd</code>.
 *
 * <p>The class <code>MatrixUtils</code> is taken from file
 * <a href="https://github.com/dberm22/Bipartite-Solver/blob/dabaf0f6fd60a4c91b27960c119ccd2435968041/src/com/dberm22/common/MatrixUtils.java">
 *     <code>/src/com/dberm22/common/MatrixUtils.java</code>
 * </a>, with some unused methods removed.
 * <br />The class <code>lapjv</code> is taken from file
 * <a href="https://github.com/dberm22/Bipartite-Solver/blob/dabaf0f6fd60a4c91b27960c119ccd2435968041/src/com/dberm22/lapjv4j/lapjv.java">
 *     <code>/src/com/dberm22/lapjv4j/lapjv.java</code>
 * </a>, with some unused methods removed. </p>
 *
 * <p>The original project is open sourced under the MIT License, which is attached below in this comment.
 * The package is not published on Maven so this may be the easiest way of using it.</p>
 *
 * <pre>
 * Copyright (c) 2012-2021 Scott Chacon and others
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * </pre>
 */

package org.automanlang.core.util;
import java.util.Arrays;

class MatrixUtils
{
    public static double[][] makeSquare(double[][] matrix, double fillValue)
    {
        int rdim = matrix.length;
        int cdim = matrix[0].length;

        if (rdim==cdim)
        {
            return transpose(matrix);
        }
        else if (rdim>cdim)
        {
            matrix = transpose(matrix);
            double[][] temp = new double[rdim-cdim][rdim];
            for (double[] row: temp) Arrays.fill(row,fillValue);
            matrix = append(matrix,temp);
            return transpose(matrix);
        }
        else
        {
            double[][] temp = new double[cdim-rdim][cdim];
            for (double[] row: temp) Arrays.fill(row,fillValue);
            return append(matrix,temp);
        }
    }

    public static double[][] transpose(double[][] matrix)
    {
        int m = matrix.length;
        int n = matrix[0].length;

        double[][] transposed = new double[n][m];

        for(int i = 0; i < m; i++)
        {
            for(int j = 0; j < n; j++)
            {
                transposed[j][i] = matrix[i][j];
            }
        }
        return transposed;
    }

    public static double sum(double[][] matrix)
    {
        double sum=0;

        int m = matrix.length;
        int n = matrix[0].length;

        for(int i = 0; i < m; i++)
        {
            for(int j = 0; j < n; j++)
            {
                sum+=matrix[i][j];
            }
        }
        return sum;
    }

    public static double[][] append(double[][] a, double[][] b)
    {
        double[][] result = new double[a.length + b.length][];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

}

/************************************************************************
 * Java port of:
 *  lap.cpp
 *   version 1.0 - 4 September 1996
 *   author: Roy Jonker @ MagicLogic Optimization Inc.
 *   e-mail: roy_jonker@magiclogic.com
 *
 *   Code for Linear Assignment Problem, according to
 *
 *   "A Shortest Augmenting Path Algorithm for Dense and Sparse Linear
 *    Assignment Problems," Computing 38, 325-340, 1987
 *
 *   by
 *
 *   R. Jonker and A. Volgenant, University of Amsterdam.
 *
 *
 *   PORTED to Java 2014-08-09 by David Berman (dberm22@gmail.com)
 *
 *************************************************************************/

public class lapjv
{
    public static int[] execute(double[][] assigncost)
    {

        // input:
        // assigncost - cost matrix

        // output:
        // sol     - list of row numbers assigned to columns (if more columns than rows), otherwise column number assigned to rows in solution.

        boolean unassignedfound;
        int  i, imin, numfree = 0, prvnumfree, f, i0, k, freerow;
        int  j, j1, j2=0, endofpath = 0, last = 0, low, up;
        double min=0.0, h, umin, usubmin, v2;
        int[] free, collist, matches, pred, rowsol, colsol;
        double[] d, v;
//	  double[] u;

        int rdim = assigncost.length;
        int cdim = assigncost[0].length;
        assigncost = MatrixUtils.makeSquare(assigncost,MatrixUtils.sum(assigncost)+1.0);
        int dim = assigncost.length;
        //int dim = Math.max(assigncost.length, assigncost[0].length);

        rowsol = new int[dim];
        colsol = new int[dim];
//	  u = new double[assigncost.length];
        v = new double[assigncost[0].length];
        free = new int[dim];       // list of unassigned rows.
        collist = new int[dim];    // list of columns to be scanned in various ways.
        matches = new int[dim];    // counts how many times a row could be assigned.
        d = new double[dim];         // 'cost-distance' in augmenting path calculation.
        pred = new int[dim];       // row-predecessor of column in augmenting/alternating path.

        // init how many times a row will be assigned in the column reduction.
        for (i = 0; i < dim; i++)
            matches[i] = 0;

        // COLUMN REDUCTION
        for (j = dim-1; j >= 0; j--)    // reverse order gives better results.
        {
            // find minimum cost over rows.
            min = assigncost[0][j];
            imin = 0;
            for (i = 1; i < dim; i++)
                if (assigncost[i][j] < min)
                {
                    min = assigncost[i][j];
                    imin = i;
                }
            v[j] = min;

            if (++matches[imin] == 1)
            {
                // init assignment if minimum row assigned for first time.
                rowsol[imin] = j;
                colsol[j] = imin;
            }
            else
                colsol[j] = -1;        // row already assigned, column not assigned.
        }

        // REDUCTION TRANSFER
        for (i = 0; i < dim; i++)
            if (matches[i] == 0)     // fill list of unassigned 'free' rows.
                free[numfree++] = i;
            else
            if (matches[i] == 1)   // transfer reduction from rows that are assigned once.
            {
                j1 = rowsol[i];
                min = Double.MAX_VALUE;
                for (j = 0; j < dim; j++)
                    if (j != j1)
                        if (assigncost[i][j] - v[j] < min)
                            min = assigncost[i][j] - v[j];
                v[j1] = v[j1] - min;
            }

        // AUGMENTING ROW REDUCTION
        int loopcnt = 0;           // do-loop to be done twice.
        do
        {
            loopcnt++;

            // scan all free rows.
            // in some cases, a free row may be replaced with another one to be scanned next.
            k = 0;
            prvnumfree = numfree;
            numfree = 0;             // start list of rows still free after augmenting row reduction.
            while (k < prvnumfree)
            {
                i = free[k];
                k++;

                // find minimum and second minimum reduced cost over columns.
                umin = assigncost[i][0] - v[0];
                j1 = 0;
                usubmin = Double.MAX_VALUE;
                for (j = 1; j < dim; j++)
                {
                    h = assigncost[i][j] - v[j];
                    if (h < usubmin)
                        if (h >= umin)
                        {
                            usubmin = h;
                            j2 = j;
                        }
                        else
                        {
                            usubmin = umin;
                            umin = h;
                            j2 = j1;
                            j1 = j;
                        }
                }

                i0 = colsol[j1];
                if (umin < usubmin)
                    // change the reduction of the minimum column to increase the minimum
                    // reduced cost in the row to the subminimum.
                    v[j1] = v[j1] - (usubmin - umin);
                else                   // minimum and subminimum equal.
                    if (i0 >= 0)         // minimum column j1 is assigned.
                    {
                        // swap columns j1 and j2, as j2 may be unassigned.
                        j1 = j2;
                        i0 = colsol[j2];
                    }

                // (re-)assign i to j1, possibly de-assigning an i0.
                rowsol[i] = j1;
                colsol[j1] = i;

                if (i0 >= 0)           // minimum column j1 assigned earlier.
                    if (umin < usubmin)
                        // put in current k, and go back to that k.
                        // continue augmenting path i - j1 with i0.
                        free[--k] = i0;
                    else
                        // no further augmenting reduction possible.
                        // store i0 in list of free rows for next phase.
                        free[numfree++] = i0;
            }
        }
        while (loopcnt < 2);       // repeat once.

        // AUGMENT SOLUTION for each free row.
        for (f = 0; f < numfree; f++)
        {
            freerow = free[f];       // start row of augmenting path.

            // Dijkstra shortest path algorithm.
            // runs until unassigned column added to shortest path tree.
            for (j = 0; j < dim; j++)
            {
                d[j] = assigncost[freerow][j] - v[j];
                pred[j] = freerow;
                collist[j] = j;        // init column list.
            }

            low = 0; // columns in 0..low-1 are ready, now none.
            up = 0;  // columns in low..up-1 are to be scanned for current minimum, now none.
            // columns in up..dim-1 are to be considered later to find new minimum,
            // at this stage the list simply contains all columns
            unassignedfound = false;
            do
            {
                if (up == low)         // no more columns to be scanned for current minimum.
                {
                    last = low - 1;

                    // scan columns for up..dim-1 to find all indices for which new minimum occurs.
                    // store these indices between low..up-1 (increasing up).
                    min = d[collist[up++]];
                    for (k = up; k < dim; k++)
                    {
                        j = collist[k];
                        h = d[j];
                        if (h <= min)
                        {
                            if (h < min)     // new minimum.
                            {
                                up = low;      // restart list at index low.
                                min = h;
                            }
                            // new index with same minimum, put on undex up, and extend list.
                            collist[k] = collist[up];
                            collist[up++] = j;
                        }
                    }

                    // check if any of the minimum columns happens to be unassigned.
                    // if so, we have an augmenting path right away.
                    for (k = low; k < up; k++)
                        if (colsol[collist[k]] < 0)
                        {
                            endofpath = collist[k];
                            unassignedfound = true;
                            break;
                        }
                }

                if (!unassignedfound)
                {
                    // update 'distances' between freerow and all unscanned columns, via next scanned column.
                    j1 = collist[low];
                    low++;
                    i = colsol[j1];
                    h = assigncost[i][j1] - v[j1] - min;

                    for (k = up; k < dim; k++)
                    {
                        j = collist[k];
                        v2 = assigncost[i][j] - v[j] - h;
                        if (v2 < d[j])
                        {
                            pred[j] = i;
                            if (v2 == min)   // new column found at same minimum value
                                if (colsol[j] < 0)
                                {
                                    // if unassigned, shortest augmenting path is complete.
                                    endofpath = j;
                                    unassignedfound = true;
                                    break;
                                }
                                // else add to list to be scanned right away.
                                else
                                {
                                    collist[k] = collist[up];
                                    collist[up++] = j;
                                }
                            d[j] = v2;
                        }
                    }
                }
            }
            while (!unassignedfound);

            // update column prices.
            for (k = 0; k <= last; k++)
            {
                j1 = collist[k];
                v[j1] = v[j1] + d[j1] - min;
            }

            // reset row and column assignments along the alternating path.
            do
            {
                i = pred[endofpath];
                colsol[endofpath] = i;
                j1 = endofpath;
                endofpath = rowsol[i];
                rowsol[i] = j1;
            }
            while (i != freerow);
        }

/*	  UNNEEDED
	  // calculate optimal cost.
	  double lapcost = 0;
	  for (i = 0; i < dim; i++)
	  {
	    j = rowsol[i];
	    u[i] = assigncost[i][j] - v[j];
	    lapcost = lapcost + assigncost[i][j];
	  }
*/

        //trim and return
        int[] sol;
        if (rdim<cdim)
        {
            sol = new int[rdim];
            for(i=0;i<rdim;i++) { sol[i]=rowsol[i]; }
        }
        else
        {
            sol = new int[cdim];
            for(i=0;i<cdim;i++) { sol[i]=colsol[i]; }
        }
        return sol;

    }

}

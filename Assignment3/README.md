------------------------------------------------------
COMP557 - Fall 2018 - Assignment 3 Mesh Simplification
------------------------------------------------------
Name: Bo Dang
ID: 260855904
Email: bo.dang@mail.mcgill.ca

-------------------------------------------------------------------
Specify implementation for each step(especially boundary problems).
-------------------------------------------------------------------

1. Use Map<String, HalfEdge> to initialize half edge data structure.

2. Detect NULL half edge in loop to avoid boundary problems.

3. Check whether {i, j} is a boundary edge when {i} and {j} are both boundary  vertex. See function isCollapse(HalfEdge).

4. Choose entry-wise operation to calculate matrix and vectors in each class.
Also detect NULL half edge in loop to avoid boundary problems.

5. See member function meshSimplification in class HEDS.

6&7. See redoCollpase() and undoCollapse(). 

8. See 1-7 above.

Should you have any question, do not hesitate to contact me at bo.dang@mail.mcgill.ca :)




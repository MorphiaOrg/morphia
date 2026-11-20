.. code-block:: javascript

   db.items.aggregate([
      { $project: { n: NumberDecimal("0.10"), text: { $substr: ["sample", 0, 2] } } }
   ]);

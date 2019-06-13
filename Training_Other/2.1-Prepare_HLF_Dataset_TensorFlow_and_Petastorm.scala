//
// Extract the High Level features training and test set from the larger datasets
// Convert Vectors into Arrays
// Save the resulting Spark DataFrames as Parquet files
// This is used as input for the TensorFlow and Petastorm example notebooks
// Run with Scala shell or a Scala notebook

// Credits: the core idea of this recipe comes mostly from
// https://docs.azuredatabricks.net/applications/deep-learning/data-prep/petastorm.html

// Define a UDF to transform Vectors in Arrays
// This is because we need Array for the Petastorm exmaple notebook

import org.apache.spark.ml.linalg.Vector
val toArray = udf { v: Vector => v.toArray }
spark.udf.register("toArray", toArray)

// Data source

val PATH = "hdfs://analytix/Training/Spark/TopologyClassifier/"
val outputPATH = PATH
val df=spark.read.parquet(PATH + "testUndersampled.parquet").select("HLF_input", "encoded_label")

scala> df.printSchema
root
 |-- HLF_input: vector (nullable = true)
 |-- encoded_label: vector (nullable = true)

// save the test dataset
// compact output in 1 file with coalesce(1)
// use a Parquet block size of 1MB, this forces row groups to 1MB and is motivated by
// later use of Petastorm make_batch_reader to determine the batch size to feed to Tensorflow
df.selectExpr("toArray(HLF_input) as HLF_input", "toArray(encoded_label) as encoded_label").
  coalesce(1).write.  
  option("parquet.block.size", 1024 * 1024).  
  parquet(outputPATH + "testUndersampled_HLF_features")

//
// repeat for the training dataset
//

val df2=spark.read.parquet(PATH + "trainUndersampled.parquet")

df2.selectExpr("toArray(HLF_input) as HLF_input", "toArray(encoded_label) as encoded_label").
  coalesce(4).write.
  option("parquet.block.size", 1024 * 1024).
  parquet(outputPATH + "trainUndersampled_HLF_features")

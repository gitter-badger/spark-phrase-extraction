import org.apache.spark.sql.SparkSession
import spark.gensim.phraser.{Phraser, Phrases, Util}

//// http://dspace.uib.no/bitstream/handle/1956/11033/lyse-andersen-mwe-final.pdf?sequence=1&isAllowed=y
object SparkGensimPhrasePredictor {

  def main(args: Array[String]): Unit = {
    println("main")

    val spark = SparkSession
      .builder
      .master("local[2]")
      .appName("streaming-gensim-phraser")
      .getOrCreate()

    val phrases = Util.load[Phrases]("/tmp/gensim-model")
    val phraserBc = spark.sparkContext.broadcast(new Phraser(phrases))

    import spark.implicits._
    val sentencesDf = spark.read
      .format("text")
      .load("/tmp/gensim-input").as[String]

    val sentenceBigramsDf = sentencesDf.map(sentence => phraserBc.value.apply(sentence.split(" ")))
    sentenceBigramsDf.write.json("/tmp/gensim-output/")

    sys.ShutdownHookThread {
      spark.stop()
    }
  }
}

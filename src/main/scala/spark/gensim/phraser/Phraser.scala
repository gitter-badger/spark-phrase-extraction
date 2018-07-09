package spark.gensim.phraser

import spark.gensim.common.{PhraserConfig, SimplePhraserConfig, Util}
import spark.gensim.phraser.Phraser.SENTENCE_TYPE
import spark.gensim.score.BigramScorer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object Phraser {
  type SENTENCE_TYPE = Array[String]
  type SENTENCES_TYPE = Array[SENTENCE_TYPE]
  type PHRASES_TYPE = Array[String]

  def main(args: Array[String]): Unit = {
    val sentence_stream = Array[Array[String]](
      "the mayor of san francisco was there".split(" "),
      "san francisco is beautiful city".split(" "),
      "machine learning can be useful sometimes".split(" "),
      "the city of san francisco is beautiful".split(" "),
      "machine learning code is in python".split(" "))

    val sentence_stream0 = Array("Human Machine interface for lab abc computer applications",
    "A survey of user opinion of computer system response time",
    "The EPS user interface management system",
    "System and Human Machine engineering testing of EPS",
    "Relation of user perceived response time to error measurement",
    "The generation of random binary unordered trees",
    "The intersection graph of paths in trees",
    "Graph minors IV Widths of trees and well quasi ordering",
    "Graph minors A survey").map(x => x.split(" "))

    val common_words= mutable.HashSet[String]("of", "with", "without", "and", "or", "the", "a")
    val phrases = new Phrases(new SimplePhraserConfig().copy(minCount=1, threshold=1.0f, commonWords = Some(common_words)), BigramScorer.getScorer(BigramScorer.DEFAULT))
    phrases.addVocab(sentence_stream)

    val bigram_phraser = Phraser(phrases)
   //  sentence_stream.foreach(sentence => println("$$$$$$$$$$: " + bigram_phraser(sentence).mkString(" ")))

    val bigrams = sentence_stream.map(sentence => bigram_phraser(sentence))

    val trigram_phrases = new Phrases(new SimplePhraserConfig().copy(minCount=1, threshold=1.0f, commonWords = Some(common_words)), BigramScorer.getScorer(BigramScorer.DEFAULT))
    trigram_phrases.addVocab(bigrams)
    val trigram_phraser = Phraser(trigram_phrases)
    bigrams.foreach(sentence => { println("##########:" + trigram_phraser(sentence).mkString(" ")) } )
  }
}

case class Phraser(phrases_model: Phrases) extends Serializable {

  val config: PhraserConfig = phrases_model.config
  val phrase_grams = new mutable.HashMap[Seq[String], (Int, Double)]()
  println("source_vocab length %d".format(phrases_model.corpus_vocab.size()))
  var count = 0
  val corpus = phrases_model.pseudoCorpus()//.map(x => x.mkString(" ")) // TODO: check this

  val bigram_scores = phrases_model.exportPhrasesAsTuples(corpus)
  for((bigram, score) <- bigram_scores) {
    val bigram_count = phrases_model.corpus_vocab.getCount(bigram.mkString(config.delimiter))
    phrase_grams.put(bigram, (bigram_count, score))

    count = count + 1
    if(count % 50000 == 0) {
      println("Phraser added %d phrasegrams".format(count))
    }
  }
  println("Phraser built with %d %d phrasegrams".format(count, phrase_grams.size))

  /**
    *  >>> sent = [u'trees', u'graph', u'minors']
        >>> print(phraser_model[sent])
        [u'trees_graph', u'minors']
        >>> sent = [['trees', 'graph', 'minors'],['graph', 'minors']]
        >>> for phrase in phraser_model[sent]:
        ...     print(phrase)
        [u'trees_graph', u'minors']
        [u'graph_minors']
    * @param sentences
    * @return list of bigrams per sentence.
    */
  def apply(sentences: Array[Array[String]]): Array[Array[String]] = {

    val output = new ListBuffer[Array[String]]()
    for(sentence <- sentences) {
      output += apply(sentence)
    }
    output.toArray
  }

  def apply(sentence: SENTENCE_TYPE): Array[String] = {

    val sentenceOutput = new ListBuffer[String]()
    val bigramScores = DefaultSentenceAnalyzer.analyze(sentence, phrases_model, phrases_model.scorer) // TODO: pass scorer. check if mkString(" ") for sentence is right
    for((bigram, score) <- bigramScores) {
      //if(score.isDefined) {
        sentenceOutput += bigram
      //}
    }
    sentenceOutput.toArray
  }
}

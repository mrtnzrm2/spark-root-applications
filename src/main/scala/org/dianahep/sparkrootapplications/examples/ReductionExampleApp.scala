package org.dianahep.sparkrootapplications.examples

import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.rdd.RDD

import scala.math._

//import org.apache.spark.implicits._

import org.dianahep.sparkroot._
import org.dianahep.sparkrootapplications.defs.aodpublic._

import org.dianahep.histogrammar._
import org.dianahep.histogrammar.ascii._

// for casting
//case class Event(particle: Seq[RecoLeafCandidate]);
case class Event(muons: Seq[RecoLeafCandidate]);


object ReductionExampleApp {
  def main(args: Array[String]) {
    val inputPath = args(0)
    val muonMass = 0.105658369
    val spark = SparkSession.builder()
      .appName("AOD Public DS Example")
      .getOrCreate()

    // get the Dataset[Row] = Dataframe (from 2.0)
    val df = spark.sqlContext.read.option("tree", "Events").root(inputPath)

    // at this point at least...
    import spark.implicits._

    // select only AOD Collection of Muons. Now you have Dataset[Event].
    val dsMuons = df.select("recoMuons_muons__RECO_.recoMuons_muons__RECO_obj.reco::RecoCandidate.reco::LeafCandidate").toDF("muons").as[Event]
    // define the empty[zero] container of Histos
    //val dsElectrons = df.select("recoGsfElectrons_gsfElectrons__RECO_.recoGsfElectrons_gsfElectrons__RECO_obj.reco::RecoCandidate.reco::LeafCandidate").toDF("ele").as[Event]

    val emptyDiCandidate = Label(
      "pt" -> Bin(200, 0, 200, {m: DiCandidate => m.pt}),
      "eta" -> Bin(48, -2.4, 2.4, {m: DiCandidate => m.eta}),
      "phi" -> Bin(63, -3.15, 3.15, {m: DiCandidate => m.phi}),
      "mass" -> Bin(200, 0, 200, {m: DiCandidate => m.mass})
    )
/*
    def filter_(e: Event, vpt: Double, veta: Double): Boolean  = {
      var pt = e.muons(0).pt
      var eta = e.muons(0).eta
      for( i <- 0 until e.muons.length; j <- 0 until e.muons.length ) {
          var muon_pt = e.muons(i).pt
	  var muon_eta = e.muons(j).eta
          if (pt >= muon_pt) { pt = muon_pt}
	  if (eta >= muon_eta){ eta = muon_eta}
      
      }
      if (pt >= vpt && math.abs(eta) < veta) {return true}
      else {return false}
     }
*/
    def filter_muon(e: Event): Boolean  = {
      if (e.muons.length > 0){
      var pt = e.muons(0).pt
      var eta = e.muons(0).eta
      for( i <- 0 until e.muons.length) {
	  for( j <- 0 until e.muons.length ) {
          	  var muon_pt = e.muons(i).pt
		  var muon_eta = e.muons(j).eta
	          if (pt >= muon_pt) { pt = muon_pt}
		  if (eta >= muon_eta){ eta = muon_eta}
	  }
      
      }
      if (pt >= 10 && math.abs(eta) < 2.4) {return true}
      else {return false}
     }
     else {return false}
     }

      val bMuons = dsMuons.filter(filter_muon _)
		.flatMap({e: Event => for (i <- 0 until e.muons.length) yield buildCandidate(e.muons(i))})
//      val dsDiMuons = dsMuons.filter(_.muons(0).pt>=10)
//      .flatMap({e: Event => for (i <- 0 until e.muons.length) yield buildCandidate(e.muons(i))})
     // val bElectrons = dsElectrons.filter(filter_(_,10,2.5)).flatMap({e: Event => for (i <- 0 until e.particle.length) yield buildCandidate(e.particle(i))})
    bMuons.show
//    dsDiMuons.show
    //bElectrons.show
   bMuons.write.format("parquet").save("file:/tmp/testReduced.parquet")
//    dsDiMuons.write.format("parquet").save("file:/tmp/testReduced.parquet")
    val filled = bMuons.rdd.aggregate(emptyDiCandidate)(new Increment, new Combine);
//    val filled = dsDiMuons.rdd.aggregate(emptyDiCandidate)(new Increment, new Combine);
    filled("pt").println;
    filled.toJsonFile("/tmp/testBundle.json")


    // stop the session/context
    spark.stop

  }
}

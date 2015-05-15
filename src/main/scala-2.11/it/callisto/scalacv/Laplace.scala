package it.callisto.scalacv

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

object Laplace extends App with OpenCVCombos {
    
  println("\nRunning LaplaceDemo")
  
  loadNativeLibs()
  
  // instantiate all independent futures before the for comprehension
  val fromFile = readImg("/Lena.png")
  val p = for {
    mat_image ← fromFile
    reduced ← reduceNoise(mat_image)
    laplace ← laplace(reduced)
    scaled ← convertScaleAbs(laplace)
    _ ← writeImg(scaled, "laplace.png")
  } yield ()
  
  Await.ready(p, 5 seconds)
    
}
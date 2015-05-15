package it.callisto.scalacv

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

object Sobel extends App with OpenCVCombos {
    
  println("\nRunning SobelDemo")
  
  loadNativeLibs()
  
  // instantiate all independent futures before the for comprehension
  val fromFile = readImg("/Lena.png")
  val p = for {
    mat_image ← fromFile
    reduced ← reduceNoise(mat_image)
    weighted ← approxGradient(reduced)
    _ ← writeImg(weighted, "sobel.png")
  } yield ()
  
  Await.ready(p, 5 seconds)
    
}
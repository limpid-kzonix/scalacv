package it.callisto.scalacv

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

object Canny extends App with OpenCVCombos {
    
  println("\nRunning CannyDemo")
  
  loadNativeLibs()
  
  // instantiate all independent futures before the for comprehension
  val fromFile = readImg("/Lena.png")
  val p = for {
    mat_image ← fromFile
    gray ← toGray(mat_image)
    blurred ← blur(gray)
    canned ← canny(blurred, 50)
    _ ← writeImg(canned, "canny.png")
  } yield ()
  
  Await.ready(p, 5 seconds)
    
}
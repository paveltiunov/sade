package org.sade.analyzers

import java.io.{BufferedInputStream, EOFException, InputStream}

class FloatReader(inputStream: InputStream) {
  val stream = new BufferedInputStream(inputStream)

  def chunkStream: Stream[Float] = readNext.map(b => Stream.cons(b, chunkStream)).getOrElse(Stream.empty)

  def chunkArrayStream: Stream[Array[Float]] = chunkStream match {
    case stream: Stream[Float] if !stream.isEmpty => Stream.cons(stream.take(32768).toArray, chunkArrayStream)
    case _ => Stream.empty
  }

  private def readNext: Option[Float] = {
    try {
      val ch1 = stream.read
      val ch2 = stream.read
      val ch3 = stream.read
      val ch4 = stream.read
      if (ch1 == -1) None
      else {
        Some(java.lang.Float.intBitsToFloat((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0)))
      }
    } catch {
      case e: EOFException => {
        stream.close()
        None
      }
    }
  }
}
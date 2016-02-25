package edu.umass.cs.automan.adapters.mturk.worker

import java.util.concurrent.atomic.AtomicLong

private object FIFOCounter {
  private val seq = new AtomicLong()
  def getNum() = seq.getAndIncrement()
}

/**
  * Adapted from http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/PriorityBlockingQueue.html.
  * The purpose of this wrapper class is so that FIFO ordering is
  * maintained for elements in the queue having the same priority.
  */
class FIFOMessage(entry: Message) extends Comparable[FIFOMessage] {
  private val _seqNum: Long = FIFOCounter.getNum()
  private val _entry: Message = entry
  def compareTo(other: FIFOMessage) : Int = {
    // do inner comparison (i.e., priority)
    val res = _entry.compareTo(other._entry)
    if (res == 0 && !other._entry.eq(_entry)) {
      // same priority; do outer comparison (i.e., sequence number)
      if (_seqNum < other._seqNum) {
        -1
      } else {
        1
      }
    } else {
      res
    }
  }
  def getEntry = _entry
}
package edu.umass.cs.automan.adapters.MTurk

case class InvalidKeyIDException(err: String) extends Exception
case class InvalidSecretKeyException(err: String) extends Exception
case class MTurkAdapterNotInitialized(err: String) extends Exception

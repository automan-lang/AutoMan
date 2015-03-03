package edu.umass.cs.automan.adapters.mturk.connectionpool

import com.amazonaws.mturk.requester.QualificationRequirement

case class HITType(id: String, quals: List[QualificationRequirement])

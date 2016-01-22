package edu.umass.cs.automan.adapters.mturk.worker

import com.amazonaws.mturk.requester.QualificationRequirement

case class HITType(id: String,
                   quals: List[QualificationRequirement],
                   disqualification: QualificationRequirement,
                   group_id: String)

package org.automanlang.adapters.mturk.worker

//import com.amazonaws.mturk.requester.QualificationRequirement
//import software.amazon.awssdk.services.mturk.model.QualificationRequirement
import com.amazonaws.services.mturk.model.QualificationRequirement

case class HITType(id: String,
                   disqualification: QualificationRequirement,
                   group_id: String)

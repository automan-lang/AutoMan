package org.automanlang.adapters.mturk.mock

import java.util.UUID

//import com.amazonaws.mturk.requester.QualificationRequirement
import com.amazonaws.services.mturk.model.QualificationRequirement

case class MockHITType(id: UUID,
                       autoApprovalDelayInSeconds: java.lang.Long,
                       assignmentDurationInSeconds: java.lang.Long,
                       reward: Double, title: String,
                       keywords: String,
                       description: String,
                       qualRequirements: Array[QualificationRequirement])

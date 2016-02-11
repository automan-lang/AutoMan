How does AutoMan ensure that all assignments are i.i.d. with respect to a question?
------------------------------------------------------------------------------------

AutoMan needs to balance the requirement that assignments be i.i.d. against workers' strong preference to work on HIT Groups with lots of assignments.  The "group_id" parameter is a user-defined key that informs AutoMan how to group work.  In general, all questions created by the same function definition should share the same group_id.

Thus AutoMan attempts to keep tasks sharing a group_id together as a single HIT Group on MTurk for as long as possible.  HIT Groups are defined by a HIT Type.  Every HIT Type is associated with a Disqualification that is granted after a worker completes any assignment in the HIT Group.  In AutoMan parlance, we call the set of HITs in the same HIT Group a "batch".

Needing more responses will not trigger the creation of a new batch.  AutoMan simply adds more assignments using the ExtendHIT call.  ExtendHIT ensures that for a given HIT, all assignments are completed by different workers.

AutoMan will create a new HIT Group for a given group_id whenever one of the following two core attributes change:
1. reward
2. timeout

The reasons are that 1) we cannot use ExtendHIT to increase reward, 2) thus we must create a new HIT, which means that 3) in order to preserve AutoMan's i.i.d. guarantee, all workers who participated in previous batches for a group_id cannot participate in the new batch.

There are two problems with this approach:

1. Since disqualifications are at the HIT Group granularity, a worker who completes an assignment is disqualified from all future assignments in subsequent batches for the same group_id.
2. If a HIT Group has any qualifications, workers must first apply.  If they have never participated before, the qualification is granted automatically, but there is a small delay for this to happen.

Batch counter optimization
--------------------------

Whenever a batch is associated with a disqualification, workers must apply before participating in that batch's HIT Group.

As a small optimization so that workers do not need to apply for a qualification when it's not possible that they have participated before (all tasks for a given group_id are brand new), the first batch has no qualifications.  I.e., AutoMan maintains a "batch counter" and when that counter == 1, the associated HIT Group has no qualifications.  However, all batches with created for counter numbers > 1 start with qualifications.

Thus the batch counter is always updated when creating NEW HITs for a given group_id.

The following scenario is a non-problem:

1. Users cannot double-submit.  It is possible for two or more HIT Groups to exist simultaneously for the same group_id.  A worker can complete a HIT in a newer HIT Group and then participate in an older HIT Group.  However, that worker cannot double-submit because a) a HIT is never available in two HIT Groups simultaneously, which means that a worker can not complete a newer and then older assignment for the same HIT, and b) completing an assignment ALWAYS issues an immediate disqualification for future instances of a HIT.

Worker whitelist
----------------


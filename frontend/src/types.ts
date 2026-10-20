// Shared TypeScript types mirroring docs/api-specification.md and docs/data-model.md exactly.
// This is the frozen contract between this frontend and the backend services — do not drift
// from these shapes without updating docs/api-specification.md first.

export type RocketStatus = 'IN_PRODUCTION' | 'INTEGRATION' | 'TEST' | 'READY' | 'LAUNCHED' | 'SCRUBBED'

export type Rocket = {
  id: number
  name: string
  variant: string
  configuration: string
  launchWindowStart: string | null
  launchWindowEnd: string | null
  status: RocketStatus
  workOrderCount: number
  completedWorkOrderCount: number
}

export type WorkOrderStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETE' | 'LATE' | 'BLOCKED'
export type WorkOrderType = 'MANUFACTURING' | 'ASSEMBLY' | 'INTEGRATION' | 'TEST' | 'LAUNCH_PREP'
export type Priority = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW'

export type Operation = {
  id: number
  workOrderId: number
  name: string
  durationMinutes: number
  requiredSkill: string
  requiredResourceType: string
  sequenceIndex: number
  status: 'PLANNED' | 'IN_PROGRESS' | 'COMPLETE' | 'BLOCKED'
  dependsOnOperationIds: number[]
}

export type WorkOrder = {
  id: number
  rocketId: number
  partNumber: string
  title: string
  workType: WorkOrderType
  priority: Priority
  dueAt: string
  status: WorkOrderStatus
  operations: Operation[]
}

export type ResourceType = 'ASSEMBLY_STATION' | 'TEST_STAND' | 'CLEAN_ROOM' | 'TECHNICIAN'
export type AvailabilityStatus = 'AVAILABLE' | 'DOWN' | 'MAINTENANCE'

export type Resource = {
  id: number
  name: string
  resourceType: ResourceType
  skills: string[]
  capacity: number
  availabilityStatus: AvailabilityStatus
}

export type Material = {
  id: number
  partNumber: string
  description: string
  quantityOnHand: number
  quantityOnOrder: number
  supplier: string | null
  expectedAt: string | null
  status: 'IN_STOCK' | 'LOW' | 'BACKORDERED'
}

export type ScheduleAssignment = {
  operationId: number
  resourceId: number
  startAt: string
  endAt: string
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETE'
}

export type ScheduleVersionStatus = 'DRAFT' | 'PROPOSED' | 'PUBLISHED' | 'SUPERSEDED' | 'REJECTED'

export type ScheduleVersion = {
  scheduleVersionId: number
  versionNumber: number
  status: ScheduleVersionStatus
  objectiveScore: number | null
  lateWorkOrders: { workOrderId: number; lateHours: number }[]
  overtimeMinutesUsed: number
  changedAssignments: number
  assignments: ScheduleAssignment[]
}

export type MachineDowntime = {
  eventId: string
  resourceId: number
  reason: string
  startedAt: string
  estimatedRecoveryAt: string | null
}

export type LaunchReadinessImpact = 'NONE' | 'AT_RISK' | 'BLOCKED'

export type AgentFindings = {
  schedulingOptimization: { candidates: ScheduleVersion[] }
  materialsReadiness: { blockedOperations: number[]; availableNow: number[] }
  bottleneckRisk: { atRiskWorkOrders: { workOrderId: number; lateProbability: number }[] }
  launchReadiness: { affectedRocketIds: number[]; readinessImpact: LaunchReadinessImpact }
  recovery: { recommendedCandidateIndex: number | null; tradeoffs: string; error?: string }
}

export type ProposalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED'

export type ScheduleProposal = {
  id: number
  status: ProposalStatus
  machineDowntime: MachineDowntime
  basedOnScheduleVersionId: number
  candidateScheduleVersionId: number | null
  agentSummary: string
  agentFindings: AgentFindings
  createdAt: string
  decidedAt: string | null
  decidedBy: string | null
}

export type ReadinessStatus = 'ON_TRACK' | 'AT_RISK' | 'BLOCKED' | 'READY_FOR_LAUNCH'
export type WeatherStatus = 'GO' | 'NO_GO' | 'UNKNOWN'

export type LaunchPreparation = {
  rocketId: number
  readinessStatus: ReadinessStatus
  readinessScore: number
  weatherStatus: WeatherStatus
  plannedLaunchAt: string | null
  notes: string
}

export type AuditEvent = {
  id: number
  eventType: string
  entityType: string
  entityId: number | null
  actor: string
  detail: Record<string, unknown>
  occurredAt: string
}

// Synthetic demo data matching the WO-101..105 scenario described in docs/architecture.md.
// This stands in for the real backend APIs (see docs/api-specification.md) until the services
// are deployed; swapping to the real API only requires implementing src/api.ts against the same
// shapes in src/types.ts — nothing in the screens themselves should need to change.

import type {
  AgentFindings,
  AuditEvent,
  LaunchPreparation,
  Material,
  Resource,
  Rocket,
  ScheduleProposal,
  ScheduleVersion,
  WorkOrder,
} from './types'

const now = new Date()
const hoursFromNow = (h: number) => new Date(now.getTime() + h * 3600_000).toISOString()
const hoursAgo = (h: number) => new Date(now.getTime() - h * 3600_000).toISOString()

export const mockRockets: Rocket[] = [
  {
    id: 1,
    name: 'New Glenn',
    variant: 'NG-4',
    configuration: '7-engine first stage, single BE-3U second stage',
    launchWindowStart: hoursFromNow(36),
    launchWindowEnd: hoursFromNow(48),
    status: 'INTEGRATION',
    workOrderCount: 5,
    completedWorkOrderCount: 1,
  },
  {
    id: 2,
    name: 'New Glenn',
    variant: 'NG-5',
    configuration: '7-engine first stage, single BE-3U second stage',
    launchWindowStart: hoursFromNow(400),
    launchWindowEnd: hoursFromNow(420),
    status: 'IN_PRODUCTION',
    workOrderCount: 2,
    completedWorkOrderCount: 0,
  },
]

export const mockResources: Resource[] = [
  { id: 7, name: 'Engine Station 01', resourceType: 'ASSEMBLY_STATION', skills: ['MACHINIST'], capacity: 1, availabilityStatus: 'DOWN' },
  { id: 8, name: 'Turbopump Station', resourceType: 'ASSEMBLY_STATION', skills: ['ASSEMBLY_TECH'], capacity: 1, availabilityStatus: 'AVAILABLE' },
  { id: 9, name: 'Valve Test Stand', resourceType: 'TEST_STAND', skills: ['QUALITY_INSPECTOR'], capacity: 1, availabilityStatus: 'AVAILABLE' },
  { id: 10, name: 'Integration Clean Room', resourceType: 'CLEAN_ROOM', skills: ['INTEGRATION_TECH'], capacity: 1, availabilityStatus: 'AVAILABLE' },
  { id: 11, name: 'Final QA Bay', resourceType: 'CLEAN_ROOM', skills: ['QUALITY_INSPECTOR'], capacity: 1, availabilityStatus: 'AVAILABLE' },
]

export const mockMaterials: Material[] = [
  { id: 1, partNumber: 'MAT-ENG-HSG', description: 'Engine housing forging', quantityOnHand: 3, quantityOnOrder: 2, supplier: 'Titan Forge Co.', expectedAt: hoursFromNow(72), status: 'IN_STOCK' },
  { id: 2, partNumber: 'MAT-TURBO-SEAL', description: 'Turbopump seal kit', quantityOnHand: 1, quantityOnOrder: 4, supplier: 'Precision Seals LLC', expectedAt: hoursFromNow(24), status: 'LOW' },
  { id: 3, partNumber: 'MAT-VALVE-ASSY', description: 'Valve assembly, flight-rated', quantityOnHand: 6, quantityOnOrder: 0, supplier: 'AeroValve Inc.', expectedAt: null, status: 'IN_STOCK' },
]

export const mockWorkOrders: WorkOrder[] = [
  {
    id: 101, rocketId: 1, partNumber: 'WO-101', title: 'Engine housing', workType: 'MANUFACTURING',
    priority: 'CRITICAL', dueAt: hoursFromNow(30), status: 'IN_PROGRESS',
    operations: [{ id: 501, workOrderId: 101, name: 'Engine housing machining', durationMinutes: 240, requiredSkill: 'MACHINIST', requiredResourceType: 'ASSEMBLY_STATION', sequenceIndex: 0, status: 'IN_PROGRESS', dependsOnOperationIds: [] }],
  },
  {
    id: 102, rocketId: 1, partNumber: 'WO-102', title: 'Turbopump assembly', workType: 'ASSEMBLY',
    priority: 'HIGH', dueAt: hoursFromNow(34), status: 'PLANNED',
    operations: [{ id: 502, workOrderId: 102, name: 'Turbopump assembly', durationMinutes: 360, requiredSkill: 'ASSEMBLY_TECH', requiredResourceType: 'ASSEMBLY_STATION', sequenceIndex: 0, status: 'PLANNED', dependsOnOperationIds: [501] }],
  },
  {
    id: 103, rocketId: 1, partNumber: 'WO-103', title: 'Valve inspection', workType: 'TEST',
    priority: 'HIGH', dueAt: hoursFromNow(26), status: 'PLANNED',
    operations: [{ id: 503, workOrderId: 103, name: 'Valve inspection', durationMinutes: 120, requiredSkill: 'QUALITY_INSPECTOR', requiredResourceType: 'TEST_STAND', sequenceIndex: 0, status: 'PLANNED', dependsOnOperationIds: [] }],
  },
  {
    id: 104, rocketId: 1, partNumber: 'WO-104', title: 'Engine integration', workType: 'INTEGRATION',
    priority: 'CRITICAL', dueAt: hoursFromNow(38), status: 'BLOCKED',
    operations: [{ id: 504, workOrderId: 104, name: 'Engine integration', durationMinutes: 300, requiredSkill: 'INTEGRATION_TECH', requiredResourceType: 'CLEAN_ROOM', sequenceIndex: 0, status: 'BLOCKED', dependsOnOperationIds: [502, 503] }],
  },
  {
    id: 105, rocketId: 1, partNumber: 'WO-105', title: 'Final quality inspection', workType: 'TEST',
    priority: 'CRITICAL', dueAt: hoursFromNow(42), status: 'PLANNED',
    operations: [{ id: 505, workOrderId: 105, name: 'Final quality inspection', durationMinutes: 180, requiredSkill: 'QUALITY_INSPECTOR', requiredResourceType: 'CLEAN_ROOM', sequenceIndex: 0, status: 'PLANNED', dependsOnOperationIds: [504] }],
  },
  {
    id: 201, rocketId: 2, partNumber: 'WO-201', title: 'Stage 1 tank weld', workType: 'MANUFACTURING',
    priority: 'MEDIUM', dueAt: hoursFromNow(380), status: 'PLANNED',
    operations: [{ id: 601, workOrderId: 201, name: 'Tank weld', durationMinutes: 480, requiredSkill: 'MACHINIST', requiredResourceType: 'ASSEMBLY_STATION', sequenceIndex: 0, status: 'PLANNED', dependsOnOperationIds: [] }],
  },
]

export const mockCurrentSchedule: ScheduleVersion = {
  scheduleVersionId: 41,
  versionNumber: 41,
  status: 'PUBLISHED',
  objectiveScore: 4.2,
  lateWorkOrders: [],
  overtimeMinutesUsed: 0,
  changedAssignments: 0,
  assignments: [
    { operationId: 501, resourceId: 7, startAt: hoursAgo(2), endAt: hoursFromNow(2), status: 'IN_PROGRESS' },
    { operationId: 502, resourceId: 8, startAt: hoursFromNow(2), endAt: hoursFromNow(8), status: 'SCHEDULED' },
    { operationId: 503, resourceId: 9, startAt: hoursAgo(1), endAt: hoursFromNow(1), status: 'SCHEDULED' },
    { operationId: 504, resourceId: 10, startAt: hoursFromNow(8), endAt: hoursFromNow(13), status: 'SCHEDULED' },
    { operationId: 505, resourceId: 11, startAt: hoursFromNow(13), endAt: hoursFromNow(16), status: 'SCHEDULED' },
    { operationId: 601, resourceId: 7, startAt: hoursFromNow(48), endAt: hoursFromNow(56), status: 'SCHEDULED' },
  ],
}

const candidateA: ScheduleVersion = {
  scheduleVersionId: 42,
  versionNumber: 42,
  status: 'PROPOSED',
  objectiveScore: 6.8,
  lateWorkOrders: [],
  overtimeMinutesUsed: 120,
  changedAssignments: 3,
  assignments: [
    { operationId: 501, resourceId: 7, startAt: hoursFromNow(3), endAt: hoursFromNow(7), status: 'SCHEDULED' },
    { operationId: 502, resourceId: 8, startAt: hoursFromNow(7), endAt: hoursFromNow(13), status: 'SCHEDULED' },
    { operationId: 503, resourceId: 9, startAt: hoursAgo(1), endAt: hoursFromNow(1), status: 'SCHEDULED' },
    { operationId: 504, resourceId: 10, startAt: hoursFromNow(13), endAt: hoursFromNow(18), status: 'SCHEDULED' },
    { operationId: 505, resourceId: 11, startAt: hoursFromNow(18), endAt: hoursFromNow(21), status: 'SCHEDULED' },
  ],
}

const candidateB: ScheduleVersion = {
  scheduleVersionId: 43,
  versionNumber: 43,
  status: 'PROPOSED',
  objectiveScore: 9.1,
  lateWorkOrders: [{ workOrderId: 104, lateHours: 3 }],
  overtimeMinutesUsed: 0,
  changedAssignments: 2,
  assignments: [
    { operationId: 501, resourceId: 7, startAt: hoursFromNow(3), endAt: hoursFromNow(7), status: 'SCHEDULED' },
    { operationId: 502, resourceId: 8, startAt: hoursFromNow(7), endAt: hoursFromNow(13), status: 'SCHEDULED' },
    { operationId: 503, resourceId: 9, startAt: hoursAgo(1), endAt: hoursFromNow(1), status: 'SCHEDULED' },
    { operationId: 504, resourceId: 10, startAt: hoursFromNow(13), endAt: hoursFromNow(18), status: 'SCHEDULED' },
    { operationId: 505, resourceId: 11, startAt: hoursFromNow(18), endAt: hoursFromNow(21), status: 'SCHEDULED' },
  ],
}

const agentFindings: AgentFindings = {
  schedulingOptimization: { candidates: [candidateA, candidateB] },
  materialsReadiness: { blockedOperations: [], availableNow: [501, 502, 503] },
  bottleneckRisk: { atRiskWorkOrders: [{ workOrderId: 104, lateProbability: 0.35 }] },
  launchReadiness: { affectedRocketIds: [1], readinessImpact: 'AT_RISK' },
  recovery: {
    recommendedCandidateIndex: 0,
    tradeoffs:
      'Candidate A protects the NG-4 launch window (36h out) at the cost of 2 hours of overtime on Turbopump Station. Candidate B avoids overtime but pushes WO-104 (critical, integration) 3 hours late, which would compress the remaining launch-prep margin to under 24 hours.',
  },
}

export const mockProposals: ScheduleProposal[] = [
  {
    id: 7,
    status: 'PENDING',
    machineDowntime: { eventId: 'evt-9001', resourceId: 7, reason: 'Preventive maintenance', startedAt: hoursAgo(0.2), estimatedRecoveryAt: hoursFromNow(3) },
    basedOnScheduleVersionId: 41,
    candidateScheduleVersionId: 42,
    agentSummary:
      'Recommend Candidate A: protects the NG-4 critical path (WO-101 \u2192 WO-104 \u2192 WO-105) and the 36-hour launch window at the cost of 2 hours overtime on Turbopump Station.',
    agentFindings,
    createdAt: hoursAgo(0.1),
    decidedAt: null,
    decidedBy: null,
  },
]

export const mockLaunchReadiness: LaunchPreparation[] = [
  {
    rocketId: 1,
    readinessStatus: 'AT_RISK',
    readinessScore: 78.5,
    weatherStatus: 'GO',
    plannedLaunchAt: hoursFromNow(36),
    notes: 'WO-104 (critical, Engine integration) at risk of a 3-hour delay pending Engine Station 01 recovery. One pending AI recovery proposal awaiting approval.',
  },
  {
    rocketId: 2,
    readinessStatus: 'ON_TRACK',
    readinessScore: 95.0,
    weatherStatus: 'UNKNOWN',
    plannedLaunchAt: hoursFromNow(400),
    notes: 'Early production phase; no disruptions affecting this vehicle.',
  },
]

export const mockAuditEvents: AuditEvent[] = [
  { id: 1001, eventType: 'WORK_ORDER_CREATED', entityType: 'WorkOrder', entityId: 101, actor: 'planner.ada', detail: { title: 'Engine housing' }, occurredAt: hoursAgo(72) },
  { id: 1002, eventType: 'WORK_ORDER_CREATED', entityType: 'WorkOrder', entityId: 104, actor: 'planner.ada', detail: { title: 'Engine integration' }, occurredAt: hoursAgo(72) },
  { id: 1003, eventType: 'SCHEDULE_PUBLISHED', entityType: 'ScheduleVersion', entityId: 41, actor: 'planner.ada', detail: { versionNumber: 41 }, occurredAt: hoursAgo(48) },
  { id: 1004, eventType: 'DOWNTIME_RECEIVED', entityType: 'MachineDowntime', entityId: 7, actor: 'system', detail: { eventId: 'evt-9001', resource: 'Engine Station 01' }, occurredAt: hoursAgo(0.2) },
  { id: 1005, eventType: 'PROPOSAL_CREATED', entityType: 'ScheduleProposal', entityId: 7, actor: 'agent-service', detail: { recommendedCandidateIndex: 0 }, occurredAt: hoursAgo(0.1) },
]

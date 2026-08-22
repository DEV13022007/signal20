// Declaration order mirrors the backend Priority enum: it is the sync drain order.
export const PRIORITIES = ['MEDICAL', 'EQUIPMENT', 'SUPPLY', 'ROUTINE']

export const PRIORITY_ORDER = PRIORITIES.reduce((acc, p, i) => ({ ...acc, [p]: i }), {})

import React from 'react';
import AuditTable from "../utils/AuditTable";

/** Settings > Activity: the changes of the settings of the signed-in project (ADMIN). */
export default function ActivityTab() {
    return <AuditTable endpoint='activity' title='Activity'/>;
}

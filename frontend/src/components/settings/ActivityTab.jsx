import React from 'react';
import AuditTable from "../utils/AuditTable";

/** Settings > Activity: the changes of the settings of the signed-in project (ADMIN). */
export default function ActivityTab() {
    // the whole screen, like the material tables of the other tabs (body + toolbar 64 + footer 52)
    return <AuditTable endpoint='activity' title='Activity' height='100vh'/>;
}

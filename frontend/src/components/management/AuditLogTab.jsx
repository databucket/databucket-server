import React, {useContext, useEffect} from 'react';
import ProjectsContext from "../../context/projects/ProjectsContext";
import AuditTable from "../utils/AuditTable";

/** Management > Audit log: the changes of all projects, their settings and the users (SUPER). */
export default function AuditLogTab() {
    const {projects, fetchProjects} = useContext(ProjectsContext);

    useEffect(() => {
        if (projects == null)
            fetchProjects();
    }, [projects, fetchProjects]);

    return <AuditTable endpoint='manage/audit' title='Audit log' projects={projects != null ? projects : []}/>;
}

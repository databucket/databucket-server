import AccessProvider from "../../context/access/AccessProvider";
import React from "react";
import ProjectData from "./_ProjectData";
import EnumsProvider from "../../context/enums/EnumsProvider";

export default function ProjectDataWrapper() {
    return (
        <AccessProvider>
            <EnumsProvider>
                <ProjectData/>
            </EnumsProvider>
        </AccessProvider>
    )
}

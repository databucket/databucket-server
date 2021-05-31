import AccessProvider from "../../context/access/AccessProvider";
import React from "react";
import ProjectData from "./_ProjectData";

export default function ProjectDataWrapper() {
    return (<AccessProvider> <ProjectData/> </AccessProvider>);
}

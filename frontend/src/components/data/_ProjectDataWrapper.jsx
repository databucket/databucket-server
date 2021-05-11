import AccessProvider from "../../context/access/AccessProvider";
import React from "react";
import ProjectData from "./_ProjectData";
import EnumsProvider from "../../context/enums/EnumsProvider";
import UsersProvider from "../../context/users/UsersProvider";

export default function ProjectDataWrapper() {
    return (
        <AccessProvider>
            <EnumsProvider>
                <UsersProvider>
                    <ProjectData/>
                </UsersProvider>
            </EnumsProvider>
        </AccessProvider>
    )
}

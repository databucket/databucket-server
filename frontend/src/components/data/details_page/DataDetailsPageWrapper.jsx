import React from 'react';
import AccessProvider from "../../../context/access/AccessProvider";
import DataDetailsPage from "./DataDetailsPage";
import TagsProvider from "../../../context/tags/TagsProvider";

export default function DataDetailsPageWrapper() {

    return (
        <AccessProvider>
            <TagsProvider>
                <DataDetailsPage/>
            </TagsProvider>
        </AccessProvider>
    );

}

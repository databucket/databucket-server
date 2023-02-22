import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import Refresh from "@mui/icons-material/Refresh";
import FilterList from "@mui/icons-material/FilterList";
import {useTheme} from "@mui/material/styles";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    getPageSizeOptions, getPostOptions, getSettingsTableHeight,
    getTableHeaderBackgroundColor, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnDescription,
    getColumnModifiedBy, getColumnModifiedAt,
    getColumnName
} from "../utils/StandardColumns";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {getBaseUrl} from "../../utils/UrlBuilder";
import TemplatesContext from "../../context/templates/TemplatesContext";
import {PlayCircleOutline} from "@mui/icons-material";

export default function TemplatesTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates, fetchProjectTemplates} = templatesContext;

    useEffect(() => {
        if (templates == null)
            fetchProjectTemplates();
    }, [templates, fetchProjectTemplates]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const runSelectedTemplate = (id) => {
        fetch(getBaseUrl('templates/run/' + id), getPostOptions())
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            });
    }

    return (
        <div>
            <MaterialTable
                title='Templates'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={templates != null ? templates : []}
                onChangeRowsPerPage={onChangeRowsPerPage}
                options={{
                    pageSize: pageSize,
                    pageSizeOptions: getPageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: 2,
                    sorting: true,
                    search: true,
                    filtering: filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                    maxBodyHeight: getSettingsTableHeight(height),
                    minBodyHeight: getSettingsTableHeight(height),
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
                actions={[
                    {
                        icon: () => <Refresh/>,
                        tooltip: 'Refresh',
                        isFreeAction: true,
                        onClick: () => fetchProjectTemplates()
                    },
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: () => setFiltering(!filtering)
                    },
                    {
                        icon: () => <PlayCircleOutline/>,
                        tooltip: 'Run',
                        onClick: (event, rowData) => runSelectedTemplate(rowData.id)
                    }
                ]}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}
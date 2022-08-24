import MaterialTable from "material-table";
import React, {createRef, useContext, useEffect, useState} from "react";
import {MessageBox} from "../utils/MessageBox";
import {
    getDeleteOptions,
    getManagementTableHeight,
    getPageSizeOptions, getPostOptions, getPutOptions, getTableHeaderBackgroundColor, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    arraysEquals,
    convertNullValuesInObject, getArrayLengthStr,
    getSelectedValues,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import SelectProjectsDialog from "../dialogs/SelectProjectsDialog";
import {useTheme} from "@material-ui/core/styles";
import {handleErrors} from "../../utils/FetchHelper";
import TemplatesContext from "../../context/templates/TemplatesContext";
import ProjectsContext from "../../context/projects/ProjectsContext";
import {
    getColumnCreatedBy,
    getColumnCreatedAt,
    getColumnModifiedBy, getColumnModifiedAt, getColumnDescription, getColumnName,
} from "../utils/StandardColumns";
import {getTemplateMapper} from "../../utils/NullValueMappers";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {getBaseUrl} from "../../utils/UrlBuilder";
import EditTemplateConfigurationDialog from "../dialogs/EditTemplateConfigurationDialog";
import SelectTemplatesDialog from "../dialogs/SelectTemplatesDialog";
import MoreHoriz from "@material-ui/icons/MoreHoriz";

export default function TemplatesTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const tableRef = createRef();
    const templatesContext = useContext(TemplatesContext);
    const {templates, fetchTemplates, addTemplate, editTemplate, removeTemplate} = templatesContext;
    const projectsContext = useContext(ProjectsContext);
    const {projects, fetchProjects, notifyProjects} = projectsContext;
    const changeableFields = ['id', 'name', 'description', 'configuration', 'templatesIds', 'projectsIds'];
    const templateSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max200']}
    };

    useEffect(() => {
        if (templates == null)
            fetchTemplates();
    }, [templates, fetchTemplates]);

    useEffect(() => {
        if (projects == null)
            fetchProjects();
    }, [projects, fetchProjects]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const getBaseTemplates = (currentTemplate, allTemplates) => {
        let filteredTemplates = JSON.parse(JSON.stringify(allTemplates));

        filteredTemplates = filteredTemplates.filter(template => {
                if (template.id === currentTemplate.id)
                    return false;

                if (template['templatesIds'] !== null)
                    if (template['templatesIds'].includes(currentTemplate.id))
                        return false;

                return true;
            }
        );

        if (filteredTemplates != null && filteredTemplates.length > 0)
            for (let newObj of filteredTemplates)
                delete newObj['tableData']

        return filteredTemplates;
    }

    return (
        <div>
            <MaterialTable
                title='Templates'
                tableRef={tableRef}
                columns={[
                    {title: '#', width: '1%', cellStyle: {width: '1%'}, render: (rowData) => rowData ? rowData.tableData.id + 1 : ''},
                    getColumnName(),
                    getColumnDescription('20%'),
                    {
                        title: 'Base templates', field: 'templatesIds', filtering: false, searchable: false, sorting: false,
                        render: rowData => getArrayLengthStr(rowData['templatesIds']),
                        editComponent: props => (
                            <SelectTemplatesDialog
                                templates={templates != null ? getBaseTemplates(props.rowData, templates) : []}
                                rowData={props.rowData}
                                onChange={props.onChange}
                            />
                        )
                    },
                    {
                        title: 'Projects', field: 'projectsIds', filtering: false, searchable: false, sorting: false,
                        render: rowData => getArrayLengthStr(rowData['projectsIds']),
                        editComponent: props => (
                            <SelectProjectsDialog
                                projects={projects != null ? projects : []}
                                rowData={props.rowData}
                                onChange={props.onChange}
                            />
                        )
                    },
                    {
                        title: 'Configuration',
                        field: 'configuration',
                        cellStyle: { width: '1%'},
                        searchable: false,
                        sorting: false,
                        render: (rowData) => <MoreHoriz/>,
                        editComponent: props => (
                            <EditTemplateConfigurationDialog
                                name={props.rowData['name'] != null ? props.rowData['name'] : ''}
                                rowData={props.rowData}
                                onChange={props.onChange}
                            />
                        ),
                        initialEditValue: {
                            "teams": [],
                            "classes": [],
                            "enums": [],
                            "groups": [],
                            "buckets": [],
                            "tags": [],
                            "columns": [],
                            "filters": [],
                            "views": [],
                            "tasks": []
                        }
                    },
                    getColumnCreatedAt(),
                    getColumnCreatedBy(),
                    getColumnModifiedAt(),
                    getColumnModifiedBy()
                ]}
                data={templates != null ? templates : []}
                onChangeRowsPerPage={onChangeRowsPerPage}
                options={{
                    pageSize: pageSize,
                    pageSizeOptions: getPageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    sorting: true,
                    search: true,
                    filtering: filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                    maxBodyHeight: getManagementTableHeight(height),
                    minBodyHeight: getManagementTableHeight(height),
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
                        onClick: () => fetchTemplates()
                    },
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: () => setFiltering(!filtering)

                    }
                ]}
                editable={{
                    onRowAdd: newData =>
                        new Promise((resolve, reject) => {
                            let message = validateItem(newData, templateSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'warning',
                                    title: 'Item is not valid',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            fetch(getBaseUrl('templates'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((template) => {
                                    if (template != null) {
                                        addTemplate(convertNullValuesInObject(template, getTemplateMapper()));
                                        notifyProjects('TEMPLATE', template.id, template['projectsIds']);
                                        resolve();
                                    }
                                });
                        }),
                    onRowUpdate: (newData, oldData) =>
                        new Promise((resolve, reject) => {
                            if (!isItemChanged(oldData, newData, changeableFields)) {
                                setMessageBox({
                                    open: true,
                                    severity: 'info',
                                    title: 'Nothing changed',
                                    message: ''
                                });
                                reject();
                                return;
                            }

                            let message = validateItem(newData, templateSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'warning',
                                    title: 'Item is not valid!',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            const payload = getSelectedValues(newData, changeableFields);

                            fetch(getBaseUrl('templates'), getPutOptions(payload))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((template) => {
                                    if (template != null) {
                                        editTemplate(convertNullValuesInObject(template, getTemplateMapper()));
                                        if (!arraysEquals(newData, oldData, 'projectsIds'))
                                            notifyProjects('TEMPLATE', template.id, template['projectsIds']);
                                        resolve();
                                    }
                                });
                        }),
                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`templates/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        e = true;
                                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        reject();
                                    })
                                    .then(() => {
                                        if (!e) {
                                            removeTemplate(oldData.id);
                                            notifyProjects('TEMPLATE', oldData.id, []);
                                            resolve();
                                        }
                                    });

                            }, 100);
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    )
}
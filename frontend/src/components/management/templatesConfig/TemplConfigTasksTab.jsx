import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import {FilterList} from "@mui/icons-material";
import {useTheme} from "@mui/material";
import {
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor,
    getTemplatePageSizeOptions,
    getTemplateTableHeight
} from "../../../utils/MaterialTableHelper";
import {
    getClassByUid,
    isItemChanged,
    validateItem
} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {
    getColumnBuckets,
    getColumnClass,
    getColumnClasses,
    getColumnCreatedAt,
    getColumnDescription,
    getColumnFilter,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../../utils/StandardColumns";
import TemplatesContext from "../../../context/templates/TemplatesContext";
import PropTypes from "prop-types";
import {getActionsType} from "../../dialogs/TaskEditConfigDialog";
import {
    getTemplatesArtefacts,
    getTemplatesArtefactsEditable,
    templateArtefactCreationEnrichment,
    templateArtefactModifyingEnrichment
} from "./_TemplUtils";
import {getClassesLookup} from "../../../utils/LookupHelper";
import TaskEditConfigDialogTemplate
    from "../../dialogs/TaskEditConfigDialogTemplate";

TemplConfigTasksTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigTasksTab(props) {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const [filters, setFilters] = useState([]);
    const [buckets, setBuckets] = useState([]);
    const [enums, setEnums] = useState([]);
    const [tags, setTags] = useState([]);
    const [classes, setClasses] = useState([]);
    const [classesLookup, setClassesLookup] = useState({});
    const [tasks, setTasks] = useState(getTemplatesArtefactsEditable(props.template, templates, 'tasks'));
    const changeableFields = ['name', 'description', 'configuration', 'filterId', 'classId', 'classesIds', 'bucketsIds'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        setFilters(getTemplatesArtefacts(props.template, templates, 'filters'));
    }, [templates]);

    useEffect(() => {
        setBuckets(getTemplatesArtefacts(props.template, templates, 'buckets'));
    }, [templates]);

    useEffect(() => {
        setEnums(getTemplatesArtefacts(props.template, templates, 'enums'));
    }, [templates]);

    useEffect(() => {
        setTags(getTemplatesArtefacts(props.template, templates, 'tags'));
    }, [templates]);

    useEffect(() => {
        const availableClasses = getTemplatesArtefacts(props.template, templates, 'classes');
        setClassesLookup(getClassesLookup(availableClasses));
        setClasses(availableClasses);
    }, [templates]);

    const onConfigurationChanged = (newTasks) => {
        setTasks(newTasks);
        const template = props.template;
        template['configuration']['tasks'] = newTasks.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    return (
        <div>
            <MaterialTable
                title='Tasks'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnClass(classesLookup, 'Class support'),
                    getColumnFilter(filters),
                    getColumnBuckets(buckets, 'Show in buckets'),
                    getColumnClasses(classes, 'Show by classes'),
                    {
                        title: 'Action',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        initialEditValue: {
                            properties: [],
                            actions: {
                                type: 'remove',
                                setTag: false,
                                tagId: 0,
                                setReserved: false,
                                reserved: false,
                                properties: []
                            }
                        },
                        render: rowData => getActionsType(rowData.configuration.actions),
                        editComponent: props => (
                            <TaskEditConfigDialogTemplate
                                rowData={props.rowData}
                                configuration={props.rowData.configuration}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                dataClass={getClassByUid(classes, props.rowData.classId)}
                                onChange={props.onChange}
                                enums={enums}
                                tags={tags}
                            />
                        )
                    },
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={tasks}
                options={{
                    pageSize: 15,
                    pageSizeOptions: getTemplatePageSizeOptions(),
                    paginationType: 'stepped',
                    actionsColumnIndex: -1,
                    sorting: true,
                    search: true,
                    filtering: filtering,
                    debounceInterval: 700,
                    padding: 'dense',
                    headerStyle: {backgroundColor: getTableHeaderBackgroundColor(theme)},
                    maxBodyHeight: getTemplateTableHeight(),
                    minBodyHeight: getTemplateTableHeight(),
                    rowStyle: rowData => ({backgroundColor: getTableRowBackgroundColor(rowData, theme)})
                }}
                components={{
                    Container: props => <div {...props} />
                }}
                actions={[
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: () => setFiltering(!filtering)
                    }
                ]}
                editable={{
                    isEditable: rowData => rowData.editable !== false,
                    isDeletable: rowData => rowData.editable !== false,
                    onRowAdd: newData =>
                        new Promise((resolve, reject) => {
                            let message = validateItem(newData, fieldsSpecification);
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

                            templateArtefactCreationEnrichment(newData);
                            onConfigurationChanged([...tasks, newData]);
                            resolve();
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

                            let message = validateItem(newData, fieldsSpecification);
                            if (message != null) {
                                setMessageBox({
                                    open: true,
                                    severity: 'error',
                                    title: 'Item is not valid',
                                    message: message
                                });
                                reject();
                                return;
                            }

                            templateArtefactModifyingEnrichment(newData);
                            onConfigurationChanged([...tasks.filter(t => t.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...tasks.filter(t => t.id !== oldData.id)]);
                            resolve();
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
        </div>
    );
}

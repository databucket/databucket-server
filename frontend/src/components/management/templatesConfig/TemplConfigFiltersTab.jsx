import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import FilterList from "@mui/icons-material/FilterList";
import {useTheme} from "@mui/material/styles";
import {
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor,
    getTemplatePageSizeOptions,
    getTemplateTableHeight
} from "../../../utils/MaterialTableHelper";
import {getClassByUid, getObjectLengthStr, isItemChanged, validateItem} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {
    getColumnClass,
    getColumnCreatedAt,
    getColumnDescription,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../../utils/StandardColumns";
import TemplatesContext from "../../../context/templates/TemplatesContext";
import PropTypes from "prop-types";
import {getClassesLookup} from "../../../utils/LookupHelper";
import EditTemplateFilterRulesDialog from "../../dialogs/EditTemplateFilterRulesDialog";
import {
    getTemplatesArtefacts,
    getTemplatesArtefactsEditable,
    templateArtefactCreationEnrichment,
    templateArtefactModifyingEnrichment
} from "./_TemplUtils";

TemplConfigFiltersTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigFiltersTab(props) {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const [classesLookup, setClassesLookup] = useState({});
    const [classes, setClasses] = useState([]);
    const [tags, setTags] = useState([]);
    const [enums, setEnums] = useState([]);
    const [filters, setFilters] = useState(getTemplatesArtefactsEditable(props.template, templates, 'filters'));
    const changeableFields = ['name', 'description', 'classId', 'configuration'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    const onConfigurationChanged = (newFilters) => {
        setFilters(newFilters);
        const template = props.template;
        template['configuration']['filters'] = newFilters.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    useEffect(() => {
        const availableClasses = getTemplatesArtefacts(props.template, templates, 'classes');

        setClassesLookup(getClassesLookup(availableClasses));
        setClasses(availableClasses);
    }, [templates]);


    useEffect(() => {
        setTags(getTemplatesArtefacts(props.template, templates, 'tags'));
    }, [templates]);

    useEffect(() => {
        setEnums(getTemplatesArtefacts(props.template, templates, 'enums'));
    }, [templates]);

    return (
        <div>
            <MaterialTable
                title='Filters'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnClass(classesLookup, 'Class support'),
                    {
                        title: 'Rules',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        initialEditValue: {properties: [], logic: null},
                        render: rowData => getObjectLengthStr(rowData['configuration']),
                        editComponent: props => (
                            <EditTemplateFilterRulesDialog
                                configuration={props.rowData.configuration}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                dataClass={getClassByUid(classes, props.rowData.classId)}
                                tags={tags}
                                users={[]}
                                onChange={props.onChange}
                                enums={enums}
                            />
                        )
                    },
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={filters}
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
                            onConfigurationChanged([...filters, newData]);
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
                            onConfigurationChanged([...filters.filter(f => f.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...filters.filter(f => f.id !== oldData.id)]);
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

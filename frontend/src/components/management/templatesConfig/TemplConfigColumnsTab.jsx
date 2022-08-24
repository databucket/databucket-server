import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {
    getTableHeaderBackgroundColor, getTableRowBackgroundColor, getTemplatePageSizeOptions, getTemplateTableHeight
} from "../../../utils/MaterialTableHelper";
import {getArrayLengthStr, getClassByUid, isItemChanged, validateItem} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {getColumnClass, getColumnCreatedAt, getColumnDescription, getColumnModifiedAt, getColumnModifiedBy, getColumnName} from "../../utils/StandardColumns";
import {useWindowDimension} from "../../utils/UseWindowDimension";
import TemplatesContext from "../../../context/templates/TemplatesContext";
import PropTypes from "prop-types";
import {getClassesLookup} from "../../../utils/LookupHelper";
import EditTemplateColumnsDialog from "../../dialogs/EditTemplateColumnsDialog";
import {getTemplatesArtefacts, getTemplatesArtefactsEditable, templateArtefactCreationEnrichment, templateArtefactModifyingEnrichment} from "./_TemplUtils";

TemplConfigColumnsTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigColumnsTab(props) {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const [classesLookup, setClassesLookup] = useState({});
    const [classes, setClasses] = useState([]);
    const [columns, setColumns] = useState(getTemplatesArtefactsEditable(props.template, templates, 'columns'));
    const changeableFields = ['name', 'description', 'configuration', 'classId'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };
    const template = props.template;

    useEffect(() => {
        const availableClasses = getTemplatesArtefacts(props.template, templates, 'classes');
        setClassesLookup(getClassesLookup(availableClasses));
        setClasses(availableClasses);
    }, [templates]);

    const onConfigurationChanged = (newColumns) => {
        setColumns(newColumns);
        const template = props.template;
        template['configuration']['columns'] = newColumns.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    return (
        <div>
            <MaterialTable
                title='Columns'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnClass(classesLookup, 'Class support'),
                    {
                        title: 'Columns',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        initialEditValue: {fields: [], columns: []},
                        render: rowData => getArrayLengthStr(rowData['configuration']['columns']),
                        editComponent: props => (
                            <EditTemplateColumnsDialog
                                template={template}
                                configuration={props.rowData.configuration != null ? props.rowData.configuration : {fields: [], columns: []}}
                                dataClass={getClassByUid(classes, props.rowData.classId)}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                onChange={props.onChange}
                            />
                        )
                    },
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={columns}
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
                    maxBodyHeight: getTemplateTableHeight(height),
                    minBodyHeight: getTemplateTableHeight(height),
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
                            onConfigurationChanged([...columns, newData]);
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
                            onConfigurationChanged([...columns.filter(col => col.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...columns.filter(col => col.id !== oldData.id)]);
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
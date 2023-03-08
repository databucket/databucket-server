import MaterialTable from "material-table";
import React, {useContext, useState} from "react";
import FilterList from "@mui/icons-material/FilterList";
import {useTheme} from "@mui/material/styles";
import {
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor,
    getTemplatePageSizeOptions,
    getTemplateTableHeight
} from "../../../utils/MaterialTableHelper";
import {getArrayLengthStr, isItemChanged, validateItem} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {
    getColumnCreatedAt,
    getColumnDescription,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName
} from "../../utils/StandardColumns";
import PropTypes from "prop-types";
import EditTemplateClassFieldsDialog from "../../dialogs/EditTemplateClassFieldsDialog";
import {
    getTemplatesArtefactsEditable,
    templateArtefactCreationEnrichment,
    templateArtefactModifyingEnrichment
} from "./_TemplUtils";
import TemplatesContext from "../../../context/templates/TemplatesContext";

TemplConfigClassesTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigClassesTab(props) {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const template = props.template;
    const [classes, setClasses] = useState(getTemplatesArtefactsEditable(props.template, templates, 'classes'));
    const changeableFields = ['name', 'description', 'configuration'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    const onConfigurationChanged = (newClasses) => {
        setClasses(newClasses);
        const template = props.template;
        template['configuration']['classes'] = newClasses.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    return (
        <div>
            <MaterialTable
                title='Classes'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    {
                        title: 'Properties',
                        field: 'configuration',
                        filtering: false,
                        searchable: false,
                        sorting: false,
                        initialEditValue: [],
                        render: rowData => getArrayLengthStr(rowData['configuration']),
                        editComponent: props => (
                            <EditTemplateClassFieldsDialog
                                template={template}
                                configuration={props.rowData.configuration != null ? props.rowData.configuration : []}
                                name={props.rowData.name != null ? props.rowData.name : ''}
                                description={props.rowData.description != null && props.rowData.description.length > 0 ? "(" + props.rowData.description + ")" : ''}
                                onChange={props.onChange}
                            />
                        )
                    },
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={classes}
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
                            onConfigurationChanged([...classes, newData]);
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
                            onConfigurationChanged([...classes.filter(cls => cls.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...classes.filter(cls => cls.id !== oldData.id)]);
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

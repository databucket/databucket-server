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
import {isItemChanged, validateItem} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {
    getColumnBuckets,
    getColumnClasses,
    getColumnColumns,
    getColumnCreatedAt,
    getColumnDescription,
    getColumnFilter,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName,
    getColumnRole,
    getColumnTeams
} from "../../utils/StandardColumns";
import TemplatesContext from "../../../context/templates/TemplatesContext";
import PropTypes from "prop-types";
import SelectMultiViewFeaturesLookup from "../../lookup/SelectMultiViewFeaturesLookup";
import {
    getTemplatesArtefacts,
    getTemplatesArtefactsEditable,
    templateArtefactCreationEnrichment,
    templateArtefactModifyingEnrichment
} from "./_TemplUtils";
import RolesContext from "../../../context/roles/RolesContext";

TemplConfigViewsTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigViewsTab(props) {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const [views, setViews] = useState(getTemplatesArtefactsEditable(props.template, templates, 'views'));
    const [columns, setColumns] = useState([]);
    const [filters, setFilters] = useState([]);
    const [buckets, setBuckets] = useState([]);
    const [classes, setClasses] = useState([]);
    const [teams, setTeams] = useState([]);
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const changeableFields = ['name', 'description', 'classesIds', 'bucketsIds', 'columnsId', 'filterId', 'featuresIds', 'grantAccess', 'roleId', 'teamsIds'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']},
        columnsId: {title: 'Columns', check: ['notEmpty', 'selected']}
    };

    useEffect(() => {
        if (roles == null)
            fetchRoles();
    }, [roles, fetchRoles]);

    useEffect(() => {
        setColumns(getTemplatesArtefacts(props.template, templates, 'columns'));
    }, [templates]);

    useEffect(() => {
        setTeams(getTemplatesArtefacts(props.template, templates, 'teams'));
    }, [templates]);

    useEffect(() => {
        setBuckets(getTemplatesArtefacts(props.template, templates, 'buckets'));
    }, [templates]);

    useEffect(() => {
        setFilters(getTemplatesArtefacts(props.template, templates, 'filters'));
    }, [templates]);

    useEffect(() => {
        setClasses(getTemplatesArtefacts(props.template, templates, 'classes'));
    }, [templates]);

    const onConfigurationChanged = (newViews) => {
        setViews(newViews);
        const template = props.template;
        template['configuration']['views'] = newViews.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    return (
        <div>
            <MaterialTable
                title='Views'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnColumns(columns),
                    getColumnFilter(filters),
                    getColumnBuckets(buckets, 'Show in buckets'),
                    getColumnClasses(classes, 'Show by classes'),
                    getColumnRole(roles, 'Access via role'),
                    getColumnTeams(teams, 'Access via team'),
                    {
                        title: 'Enabled features', field: 'featuresIds', filtering: false, sorting: false,// initialEditValue: [],
                        render: rowData => rowData.featuresIds != null ? `[${rowData.featuresIds.length}]` : '[0]',
                        editComponent: props => <SelectMultiViewFeaturesLookup rowData={props.rowData}
                                                                               onChange={props.onChange}/>
                    },
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={views}
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
                            onConfigurationChanged([...views, newData]);
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
                            onConfigurationChanged([...views.filter(v => v.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...views.filter(v => v.id !== oldData.id)]);
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

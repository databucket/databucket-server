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
    getColumnCreatedAt,
    getColumnDescription,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName,
    getColumnRole,
    getColumnShortName,
    getColumnTeams
} from "../../utils/StandardColumns";
import TemplatesContext from "../../../context/templates/TemplatesContext";
import PropTypes from "prop-types";
import RolesContext from "../../../context/roles/RolesContext";
import {
    getTemplatesArtefacts,
    getTemplatesArtefactsEditable,
    templateArtefactCreationEnrichment,
    templateArtefactModifyingEnrichment
} from "./_TemplUtils";

TemplConfigGroupsTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigGroupsTab(props) {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const [groups, setGroups] = useState(getTemplatesArtefactsEditable(props.template, templates, 'groups'));
    const [teams, setTeams] = useState([]);
    const changeableFields = ['name', 'shortName', 'description', 'roleId', 'teamsIds'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['min5', 'max30']},
        shortName: {title: 'Short name', check: ['notEmpty', 'min1', 'max5']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (roles == null)
            fetchRoles();
    }, [roles, fetchRoles]);

    useEffect(() => {
        setTeams(getTemplatesArtefacts(props.template, templates, 'teams'));
    }, [templates]);

    const onConfigurationChanged = (newGroups) => {
        setGroups(newGroups);
        const template = props.template;
        template['configuration']['groups'] = newGroups.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    return (
        <div>
            <MaterialTable
                title='Groups'
                tableRef={tableRef}
                columns={[
                    getColumnShortName(),
                    getColumnName(),
                    getColumnDescription(),
                    getColumnRole(roles, 'Access via role'),
                    getColumnTeams(teams, 'Access via team'),
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={groups}
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
                            onConfigurationChanged([...groups, newData]);
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
                            onConfigurationChanged([...groups.filter(g => g.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...groups.filter(g => g.id !== oldData.id)]);
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

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
    getColumnClass,
    getColumnCreatedAt,
    getColumnDescription,
    getColumnGroups,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName,
    getColumnRole,
    getColumnTeams
} from "../../utils/StandardColumns";
import {useWindowDimension} from "../../utils/UseWindowDimension";
import TemplatesContext from "../../../context/templates/TemplatesContext";
import PropTypes from "prop-types";
import SelectIconDialog from "../../dialogs/SelectIconDialog";
import RolesContext from "../../../context/roles/RolesContext";
import {getClassesLookup} from "../../../utils/LookupHelper";
import {
    getTemplatesArtefacts,
    getTemplatesArtefactsEditable,
    templateArtefactCreationEnrichment,
    templateArtefactModifyingEnrichment
} from "./_TemplUtils";
import StyledIcon from "../../utils/StyledIcon";

TemplConfigBucketsTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigBucketsTab(props) {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const [teams, setTeams] = useState([]);
    const [groups, setGroups] = useState([]);
    const [classesLookup, setClassesLookup] = useState({});
    const [buckets, setBuckets] = useState(getTemplatesArtefactsEditable(props.template, templates, 'buckets'));
    const changeableFields = ['name', 'iconName', 'history', 'protectedData', 'description', 'groupsIds', 'classId', 'roleId', 'teamsIds'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (roles == null)
            fetchRoles();
    }, [roles, fetchRoles]);

    useEffect(() => {
        setTeams(getTemplatesArtefacts(props.template, templates, 'teams'));
    }, [templates]);

    useEffect(() => {
        setGroups(getTemplatesArtefacts(props.template, templates, 'groups'));
    }, [templates]);

    useEffect(() => {
        setClassesLookup(getClassesLookup(getTemplatesArtefacts(props.template, templates, 'classes')));
    }, [templates]);


    const onConfigurationChanged = (newBuckets) => {
        setBuckets(newBuckets);
        const template = props.template;
        template['configuration']['buckets'] = newBuckets.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    return (
        <div>
            <MaterialTable
                title='Buckets'
                tableRef={tableRef}
                columns={[
                    {
                        title: 'Icon',
                        sorting: false,
                        field: 'iconName',
                        searchable: false,
                        filtering: false,
                        initialEditValue: 'panorama_fish_eye',
                        render: rowData => <StyledIcon
                            iconName={rowData.icon.name}
                            iconColor={rowData.icon.color}
                            iconSvg={rowData.icon.svg}
                        />,
                        editComponent: props => <SelectIconDialog icon={props.value} onChange={props.onChange}/>
                    },
                    getColumnName(),
                    getColumnDescription(),
                    getColumnClass(classesLookup, 'Class support'),
                    getColumnGroups(groups, 'Show in groups'),
                    {title: 'Protect orphaned data', field: 'protectedData', type: 'boolean'},
                    {title: 'Collect data history', field: 'history', type: 'boolean'},
                    getColumnRole(roles, 'Access via role'),
                    getColumnTeams(teams, 'Access via teams'),
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={buckets}
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
                            onConfigurationChanged([...buckets, newData]);
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
                            onConfigurationChanged([...buckets.filter(bucket => bucket.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...buckets.filter(bucket => bucket.id !== oldData.id)]);
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

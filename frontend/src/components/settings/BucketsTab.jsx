import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import Refresh from "@mui/icons-material/Refresh";
import FilterList from "@mui/icons-material/FilterList";
import {useTheme} from "@mui/material/styles";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    getDeleteOptions,
    getPageSizeOptions,
    getPostOptions,
    getPutOptions,
    getSettingsTableHeight,
    getTableHeaderBackgroundColor,
    getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {arraysEquals, convertNullValuesInObject, isItemChanged, validateItem} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnClass,
    getColumnDescription,
    getColumnGroups,
    getColumnModifiedAt,
    getColumnModifiedBy,
    getColumnName,
    getColumnRole,
    getColumnTeams,
    getColumnUsers
} from "../utils/StandardColumns";
import BucketsContext from "../../context/buckets/BucketsContext";
import GroupsContext from "../../context/groups/GroupsContext";
import SelectIconDialog from "../dialogs/SelectIconDialog";
import RolesContext from "../../context/roles/RolesContext";
import UsersContext from "../../context/users/UsersContext";
import {getBucketMapper} from "../../utils/NullValueMappers";
import ConfirmRemovingDialog from "../utils/ConfirmRemovingDialog";
import ClassesContext from "../../context/classes/ClassesContext";
import TeamsContext from "../../context/teams/TeamsContext";
import {getBaseUrl} from "../../utils/UrlBuilder";
import SvgContext from "../../context/svgs/SvgContext";
import StyledIcon from "../utils/StyledIcon";

const EditComponent = props => <SelectIconDialog icon={props.value} onChange={props.onChange}/>;

export default function BucketsTab() {

    const theme = useTheme();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [confirmRemove, setConfirmRemove] = useState({open: false, id: 0, name: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const usersContext = useContext(UsersContext);
    const {users, fetchUsers, notifyUsers} = usersContext;
    const groupContext = useContext(GroupsContext);
    const {groups, fetchGroups, notifyGroups} = groupContext;
    const bucketsContext = useContext(BucketsContext);
    const {buckets, fetchBuckets, addBucket, editBucket, removeBucket} = bucketsContext;
    const classesContext = useContext(ClassesContext);
    const {classes, fetchClasses, classesLookup} = classesContext;
    const teamsContext = useContext(TeamsContext);
    const {teams, fetchTeams} = teamsContext;
    const svgContext = useContext(SvgContext);
    const {svgs, fetchSvgs} = svgContext;
    const changeableFields = ['name', 'icon', 'history', 'protectedData', 'description', 'groupsIds', 'usersIds', 'classId', 'roleId', 'teamsIds'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (roles == null)
            fetchRoles();
    }, [roles, fetchRoles]);

    useEffect(() => {
        if (groups == null)
            fetchGroups();
    }, [groups, fetchGroups]);

    useEffect(() => {
        if (users == null)
            fetchUsers();
    }, [users, fetchUsers]);

    useEffect(() => {
        if (buckets == null)
            fetchBuckets();
    }, [buckets, fetchBuckets]);

    useEffect(() => {
        if (classes == null)
            fetchClasses();
    }, [classes, fetchClasses]);

    useEffect(() => {
        if (teams == null)
            fetchTeams();
    }, [teams, fetchTeams]);

    useEffect(() => {
        if (svgs == null)
            fetchSvgs();
    }, [svgs, fetchSvgs]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const onRemove = (remove) => {
        if (remove) {
            let e = false;
            fetch(getBaseUrl(`buckets/${confirmRemove.id}`), getDeleteOptions())
                .then(handleErrors)
                .catch(error => {
                    e = true;
                    if (error.includes('already used by items'))
                        setMessageBox({
                            open: true,
                            severity: 'warning',
                            title: 'Item can not be removed',
                            message: error
                        });
                    else
                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                })
                .then(() => {
                    if (!e) {
                        removeBucket(confirmRemove.id);
                        notifyGroups('BUCKET', confirmRemove.id, []);
                        notifyUsers('BUCKET', confirmRemove.id, []);
                    }
                });
        }

        setConfirmRemove({open: false, id: 0, name: ''});
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
                        field: 'icon',
                        searchable: false,
                        filtering: false,
                        initialEditValue: {"name": "trip_origin", "color": null, "svg": null},
                        render: rowData => <StyledIcon iconName={rowData.icon.name}
                                                       iconColor={rowData.icon.color}
                                                       iconSvg={rowData.icon.svg}/>,
                        editComponent: EditComponent
                    },
                    getColumnName("20%"),
                    getColumnDescription("20%"),
                    getColumnClass(classesLookup, 'Class support'),
                    getColumnGroups(groups, 'Show in groups'),
                    {title: 'Protect orphaned data', field: 'protectedData', type: 'boolean'},
                    {title: 'Collect data history', field: 'history', type: 'boolean'},
                    getColumnUsers(users, roles, 'Access for users'),
                    getColumnRole(roles, 'Access via role'),
                    getColumnTeams(teams, 'Access via teams'),
                    // getColumnCreatedBy(),
                    // getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={buckets != null ? buckets : []}
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
                    maxBodyHeight: getSettingsTableHeight(),
                    minBodyHeight: getSettingsTableHeight(),
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
                        onClick: () => fetchBuckets()
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

                            fetch(getBaseUrl('buckets'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((bucket) => {
                                    if (bucket != null) {
                                        addBucket(convertNullValuesInObject(bucket, getBucketMapper()));
                                        notifyGroups('BUCKET', bucket.id, bucket['groupsIds']);
                                        notifyUsers('BUCKET', bucket.id, bucket['usersIds']);
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

                            fetch(getBaseUrl('buckets'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((bucket) => {
                                    if (bucket != null) {
                                        editBucket(convertNullValuesInObject(bucket, getBucketMapper()));
                                        if (!arraysEquals(newData, oldData, 'groupsIds'))
                                            notifyGroups('BUCKET', bucket.id, bucket['groupsIds']);
                                        if (!arraysEquals(newData, oldData, 'usersIds'))
                                            notifyUsers('BUCKET', bucket.id, bucket['usersIds']);
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve) => {
                            setConfirmRemove({open: true, id: oldData.id, name: oldData.name});
                            notifyGroups('BUCKET', oldData.id, []);
                            notifyUsers('BUCKET', oldData.id, []);
                            resolve();
                        }),
                }}
            />
            <MessageBox
                config={messageBox}
                onClose={() => setMessageBox({...messageBox, open: false})}
            />
            <ConfirmRemovingDialog
                open={confirmRemove.open}
                name={confirmRemove.name}
                message={'Remove bucket:'}
                onClose={(remove) => onRemove(remove)}
            />
        </div>
    )
}

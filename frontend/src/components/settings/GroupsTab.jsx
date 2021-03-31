import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    getDeleteOptions,
    getPageSizeOptions, getPostOptions, getPutOptions, getSettingsTableHeight,
    getTableHeaderBackgroundColor,
    getTableIcons, getTableRowBackgroundColor
} from "../../utils/MaterialTableHelper";
import {handleErrors} from "../../utils/FetchHelper";
import {
    arraysEquals,
    convertNullValuesInObject,
    isItemChanged,
    validateItem
} from "../../utils/JsonHelper";
import {MessageBox} from "../utils/MessageBox";
import {
    getColumnBuckets,
    getColumnDescription,
    getColumnModifiedBy, getColumnModifiedAt,
    getColumnName, getColumnRole, getColumnShortName, getColumnTeams, getColumnUsers
} from "../utils/StandardColumns";
import BucketsContext from "../../context/buckets/BucketsContext";
import GroupsContext from "../../context/groups/GroupsContext";
import UsersContext from "../../context/users/UsersContext";
import RolesContext from "../../context/roles/RolesContext";
import {getGroupMapper} from "../../utils/NullValueMappers";
import {useWindowDimension} from "../utils/UseWindowDimension";
import TeamsContext from "../../context/teams/TeamsContext";
import {getBaseUrl} from "../../utils/UrlBuilder";

export default function GroupsTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const groupContext = useContext(GroupsContext);
    const {groups, fetchGroups, addGroup, editGroup, removeGroup} = groupContext;
    const bucketsContext = useContext(BucketsContext);
    const {buckets, fetchBuckets, notifyBuckets} = bucketsContext;
    const usersContext = useContext(UsersContext);
    const {users, fetchUsers, notifyUsers} = usersContext;
    const teamsContext = useContext(TeamsContext);
    const {teams, fetchTeams} = teamsContext;
    const changeableFields = ['name', 'shortName', 'description', 'bucketsIds', 'usersIds', 'roleId', 'teamsIds'];
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
        if (groups == null)
            fetchGroups();
    }, [groups, fetchGroups]);

    useEffect(() => {
        if (buckets == null)
            fetchBuckets();
    }, [buckets, fetchBuckets]);

    useEffect(() => {
        if (users == null)
            fetchUsers();
    }, [users, fetchUsers]);

    useEffect(() => {
        if (teams == null)
            fetchTeams();
    }, [teams, fetchTeams]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    return (
        <div>
            <MaterialTable
                icons={getTableIcons()}
                title='Groups'
                tableRef={tableRef}
                columns={[
                    getColumnShortName(),
                    getColumnName(),
                    getColumnDescription(),
                    getColumnBuckets(buckets, 'Assigned buckets'),
                    getColumnUsers(users, roles, 'Access for user'),
                    getColumnRole(roles, 'Access by role'),
                    getColumnTeams(teams, 'Access by team'),
                    // getColumnCreatedBy(),
                    // getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={groups != null ? groups : []}
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
                        onClick: () => fetchGroups()
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

                            fetch(getBaseUrl('groups'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((group) => {
                                    if (group != null) {
                                        addGroup(convertNullValuesInObject(group, getGroupMapper()));
                                        notifyUsers('GROUP', group.id, group['usersIds']);
                                        notifyBuckets('GROUP', group.id, group['bucketsIds']);
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

                            fetch(getBaseUrl('groups'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((group) => {
                                    if (group != null) {
                                        editGroup(convertNullValuesInObject(group, getGroupMapper()));
                                        if (!arraysEquals(newData, oldData, 'usersIds'))
                                            notifyUsers('GROUP', group.id, group['usersIds']);
                                        if (!arraysEquals(newData, oldData, 'bucketsIds'))
                                            notifyBuckets('GROUP', group.id, group['bucketsIds']);
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                fetch(getBaseUrl(`groups/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        reject();
                                    })
                                    .then(() => {
                                        removeGroup(oldData.id);
                                        notifyUsers('GROUP', oldData.id, []);
                                        notifyBuckets('GROUP', oldData.id, []);
                                        resolve();
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
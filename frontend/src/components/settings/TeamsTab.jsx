import MaterialTable from "material-table";
import React, {useContext, useEffect, useState} from "react";
import Refresh from "@material-ui/icons/Refresh";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {getLastPageSize, setLastPageSize} from "../../utils/ConfigurationStorage";
import {
    getDeleteOptions,
    getPageSizeOptions, getPostOptions, getPutOptions, getSettingsTableHeight,
    getTableHeaderBackgroundColor, getTableRowBackgroundColor
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
    getColumnDescription,
    getColumnModifiedBy, getColumnModifiedAt,
    getColumnName, getColumnUsers
} from "../utils/StandardColumns";
import TeamsContext from "../../context/teams/TeamsContext";
import UsersContext from "../../context/users/UsersContext";
import RolesContext from "../../context/roles/RolesContext";
import {getTeamMapper} from "../../utils/NullValueMappers";
import {useWindowDimension} from "../utils/UseWindowDimension";
import {getBaseUrl} from "../../utils/UrlBuilder";
import BucketsContext from "../../context/buckets/BucketsContext";
import ViewsContext from "../../context/views/ViewsContext";
import GroupsContext from "../../context/groups/GroupsContext";
// import TableIcons from "../utils/TableIcons";

export default function TeamsTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const teamContext = useContext(TeamsContext);
    const {teams, fetchTeams, addTeam, editTeam, removeTeam} = teamContext;
    const usersContext = useContext(UsersContext);
    const {users, fetchUsers, notifyUsers} = usersContext;
    const bucketContext = useContext(BucketsContext);
    const {notifyBuckets} = bucketContext;
    const viewsContext = useContext(ViewsContext);
    const {notifyViews} = viewsContext;
    const groupsContext = useContext(GroupsContext);
    const {notifyGroups} = groupsContext;
    const changeableFields = ['name', 'description', 'usersIds'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    useEffect(() => {
        if (roles == null)
            fetchRoles();
    }, [roles, fetchRoles]);

    useEffect(() => {
        if (teams == null)
            fetchTeams();
    }, [teams, fetchTeams]);

    useEffect(() => {
        if (users == null)
            fetchUsers();
    }, [users, fetchUsers]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    return (
        <div>
            <MaterialTable
                // icons={TableIcons}
                title='Teams'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    getColumnUsers(users, roles),
                    // getColumnCreatedBy(),
                    // getColumnCreatedAt(),
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={teams != null ? teams : []}
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
                        onClick: () => fetchTeams()
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

                            fetch(getBaseUrl('teams'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((team) => {
                                    if (team != null) {
                                        addTeam(convertNullValuesInObject(team, getTeamMapper()));
                                        notifyUsers('TEAM', team.id, team['usersIds']);
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

                            fetch(getBaseUrl('teams'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((team) => {
                                    if (team != null) {
                                        editTeam(convertNullValuesInObject(team, getTeamMapper()));
                                        if (!arraysEquals(newData, oldData, 'usersIds'))
                                            notifyUsers('TEAM', team.id, team['usersIds']);
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`teams/${oldData.id}`), getDeleteOptions())
                                    .then(handleErrors)
                                    .catch(error => {
                                        e = true;
                                        if (error.includes('already used by items'))
                                            setMessageBox({open: true, severity: 'warning', title: 'Item can not be removed', message: error});
                                        else
                                            setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                        reject();
                                    })
                                    .then(() => {
                                        if (!e) {
                                            removeTeam(oldData.id);
                                            notifyUsers('TEAM', oldData.id, []);
                                            notifyBuckets('TEAM', oldData.id, []);
                                            notifyGroups('TEAM', oldData.id, []);
                                            notifyViews('TEAM', oldData.id, []);
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
    );
}
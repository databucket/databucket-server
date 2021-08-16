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
    getColumnBuckets,
    getColumnClasses, getColumnColumns,
    getColumnDescription, getColumnFilter,
    getColumnModifiedBy, getColumnModifiedAt,
    getColumnName, getColumnRole, getColumnTeams, getColumnUsers
} from "../utils/StandardColumns";
import {getViewsMapper} from "../../utils/NullValueMappers";
import ViewsContext from "../../context/views/ViewsContext";
import UsersContext from "../../context/users/UsersContext";
import RolesContext from "../../context/roles/RolesContext";
import ClassesContext from "../../context/classes/ClassesContext";
import BucketsContext from "../../context/buckets/BucketsContext";
import ColumnsContext from "../../context/columns/ColumnsContext";
import FiltersContext from "../../context/filters/FiltersContext";
import {useWindowDimension} from "../utils/UseWindowDimension";
import TeamsContext from "../../context/teams/TeamsContext";
import {getBaseUrl} from "../../utils/UrlBuilder";
import SelectMultiViewFeaturesLookup from "../lookup/SelectMultiViewFeaturesLookup";
import CloneIcon from "@material-ui/icons/ViewStream";

export default function ViewsTab() {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [pageSize, setPageSize] = useState(getLastPageSize);
    const [filtering, setFiltering] = useState(false);
    const usersContext = useContext(UsersContext);
    const {users, fetchUsers, notifyUsers} = usersContext;
    const rolesContext = useContext(RolesContext);
    const {roles, fetchRoles} = rolesContext;
    const classesContext = useContext(ClassesContext);
    const {classes, fetchClasses} = classesContext;
    const columnsContext = useContext(ColumnsContext);
    const {columns, fetchColumns} = columnsContext;
    const bucketsContext = useContext(BucketsContext);
    const {buckets, fetchBuckets} = bucketsContext;
    const filtersContext = useContext(FiltersContext);
    const {filters, fetchFilters} = filtersContext;
    const teamsContext = useContext(TeamsContext);
    const {teams, fetchTeams} = teamsContext;
    const viewsContext = useContext(ViewsContext);
    const {views, fetchViews, addView, editView, removeView} = viewsContext;
    const changeableFields = ['name', 'description', 'classesIds', 'bucketsIds', 'usersIds', 'columnsId', 'filterId', 'featuresIds', 'grantAccess', 'roleId', 'teamsIds'];
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
        if (users == null)
            fetchUsers();
    }, [users, fetchUsers]);

    useEffect(() => {
        if (views == null)
            fetchViews();
    }, [views, fetchViews]);

    useEffect(() => {
        if (classes == null)
            fetchClasses();
    }, [classes, fetchClasses]);

    useEffect(() => {
        if (buckets == null)
            fetchBuckets();
    }, [buckets, fetchBuckets]);

    useEffect(() => {
        if (columns == null)
            fetchColumns();
    }, [columns, fetchColumns]);

    useEffect(() => {
        if (filters == null)
            fetchFilters();
    }, [filters, fetchFilters]);

    useEffect(() => {
        if (teams == null)
            fetchTeams();
    }, [teams, fetchTeams]);

    const onChangeRowsPerPage = (pageSize) => {
        setPageSize(pageSize);
        setLastPageSize(pageSize);
    }

    const cloneItem = (rowData) => {
        fetch(getBaseUrl('views'), getPostOptions(rowData))
            .then(handleErrors)
            .catch(error => {
                setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
            })
            .then((views) => {
                if (views != null) {
                    addView(convertNullValuesInObject(views, getViewsMapper()));
                }
            });
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
                    getColumnUsers(users, roles, 'Access for user'),
                    getColumnRole(roles, 'Access via role'),
                    getColumnTeams(teams, 'Access via team'),
                    {
                        title: 'Enabled features', field: 'featuresIds', filtering: false, sorting: false,// initialEditValue: [],
                        render: rowData => rowData.featuresIds != null ? `[${rowData.featuresIds.length}]` : '[0]',
                        editComponent: props => <SelectMultiViewFeaturesLookup rowData={props.rowData} onChange={props.onChange}/>
                    },
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={views != null ? views : []}
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
                        onClick: () => fetchViews()
                    },
                    {
                        icon: () => <FilterList/>,
                        tooltip: 'Enable/disable filter',
                        isFreeAction: true,
                        onClick: () => setFiltering(!filtering)
                    },
                    {
                        icon: () => <CloneIcon/>,
                        tooltip: 'Duplicate',
                        onClick: (event, rowData) => cloneItem(rowData)
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

                            fetch(getBaseUrl('views'), getPostOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    reject();
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                })
                                .then((view) => {
                                    if (view != null) {
                                        addView(convertNullValuesInObject(view, getViewsMapper()));
                                        notifyUsers('VIEW', view.id, view['usersIds']);
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

                            fetch(getBaseUrl('views'), getPutOptions(newData))
                                .then(handleErrors)
                                .catch(error => {
                                    setMessageBox({open: true, severity: 'error', title: 'Error', message: error});
                                    reject();
                                })
                                .then((view) => {
                                    if (view != null) {
                                        editView(convertNullValuesInObject(view, getViewsMapper()));
                                        if (!arraysEquals(newData, oldData, 'usersIds'))
                                            notifyUsers('VIEW', view.id, view['usersIds']);
                                        resolve();
                                    }
                                });
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            setTimeout(() => {
                                let e = false;
                                fetch(getBaseUrl(`views/${oldData.id}`), getDeleteOptions())
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
                                            removeView(oldData.id);
                                            notifyUsers('VIEW', oldData.id, []);
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
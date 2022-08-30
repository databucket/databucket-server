import MaterialTable from "material-table";
import React, {useContext, useState} from "react";
import FilterList from "@material-ui/icons/FilterList";
import {useTheme} from "@material-ui/core/styles";
import {
    getTableHeaderBackgroundColor, getTableRowBackgroundColor, getTemplatePageSizeOptions, getTemplateTableHeight
} from "../../../utils/MaterialTableHelper";
import {isItemChanged, validateItem} from "../../../utils/JsonHelper";
import {MessageBox} from "../../utils/MessageBox";
import {getColumnCreatedAt, getColumnDescription, getColumnModifiedAt, getColumnModifiedBy, getColumnName} from "../../utils/StandardColumns";
import {useWindowDimension} from "../../utils/UseWindowDimension";
import PropTypes from "prop-types";
import {getTemplatesArtefactsEditable, templateArtefactCreationEnrichment, templateArtefactModifyingEnrichment} from "./_TemplUtils";
import TemplatesContext from "../../../context/templates/TemplatesContext";

TemplConfigTeamsTab.propTypes = {
    template: PropTypes.object.isRequired,
    setTemplate: PropTypes.func.isRequired
}

export default function TemplConfigTeamsTab(props) {

    const theme = useTheme();
    const [height] = useWindowDimension();
    const tableRef = React.createRef();
    const templatesContext = useContext(TemplatesContext);
    const {templates} = templatesContext;
    const [messageBox, setMessageBox] = useState({open: false, severity: 'error', title: '', message: ''});
    const [filtering, setFiltering] = useState(false);
    const [teams, setTeams] = useState(getTemplatesArtefactsEditable(props.template, templates, 'teams'));
    const changeableFields = ['name', 'description', 'createdAt', 'createdBy', 'modifiedAt', 'modifiedBy'];
    const fieldsSpecification = {
        name: {title: 'Name', check: ['notEmpty', 'min1', 'max30']},
        description: {title: 'Description', check: ['max250']}
    };

    const onConfigurationChanged = (teams) => {
        setTeams(teams);
        const template = props.template;
        template['configuration']['teams'] = teams.filter(item => !(item.editable === false));
        props.setTemplate(template);
    }

    return (
        <div>
            <MaterialTable
                title='Teams'
                tableRef={tableRef}
                columns={[
                    getColumnName(),
                    getColumnDescription(),
                    {...getColumnCreatedAt(), defaultSort: 'asc', hidden: true},
                    getColumnModifiedBy(),
                    getColumnModifiedAt()
                ]}
                data={teams}
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
                            onConfigurationChanged([...teams, newData]);
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
                            onConfigurationChanged([...teams.filter(team => team.id !== oldData.id), newData]);
                            resolve();
                        }),

                    onRowDelete: oldData =>
                        new Promise((resolve, reject) => {
                            onConfigurationChanged([...teams.filter(team => team.id !== oldData.id)]);
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
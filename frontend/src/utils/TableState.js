import {useRef, useState} from "react";
import {getTablePageSize, getTableState, setTablePageSize, setTableState} from "./ConfigurationStorage";

// Remembers a table with local data (Settings, Management): search, sort, filters and page until the browser tab
// is closed, rows per page permanently. Use the returned values in MaterialTable:
//   columns={tableState.columns([...])}  data={tableState.data(rows)}, then key={tableState.key}
//   and options={{...tableState.options, filtering: tableState.filtering, ...}}  {...tableState.callbacks}
//   and tableState.toggleFiltering in the filter action
export default function useTableState(tableKey) {
    // a table starts with the saved state; MaterialTable reads it only when it is created
    const saved = getTableState(tableKey);
    const [filtering, setFiltering] = useState(saved.filtering === true);
    const [pageSize, setPageSize] = useState(() => getTablePageSize(tableKey));
    const page = useRef(saved.page || 0);
    const tableVersion = useRef(0);

    const save = (changes) => setTableState(tableKey, changes);

    const savePage = (newPage) => {
        page.current = newPage;
        save({page: newPage});
    }

    const toggleFiltering = () => {
        save({filtering: !filtering});
        setFiltering(!filtering);
    }

    const columns = (tableColumns) => tableColumns.map((column, index) => {
        let result = column;
        if (saved.order != null && saved.order.colId === index)
            result = {...result, defaultSort: saved.order.ord};
        if (saved.filters != null && saved.filters[column.field] !== undefined)
            result = {...result, defaultFilter: saved.filters[column.field]};
        return result;
    });

    // rows may have been removed since the page was shown: a new table (key) starts on the last existing page
    const data = (rows) => {
        const lastPage = Math.max(0, Math.ceil(rows.length / pageSize) - 1);
        if (rows.length > 0 && page.current > lastPage) {
            savePage(lastPage);
            tableVersion.current += 1;
        }
        return rows;
    }

    return {
        filtering,
        toggleFiltering,
        columns,
        data,
        // getters: read after data(rows) in the same render
        get key() {
            return tableVersion.current;
        },
        get options() {
            return {
                pageSize: pageSize,
                initialPage: page.current,
                searchText: saved.searchText || ''
            };
        },
        callbacks: {
            // the table goes back to the first page after a search, sort or filter change
            onSearchChange: searchText => {
                save({searchText: searchText});
                savePage(0);
            },
            onOrderChange: (colId, ord) => {
                save({order: colId >= 0 ? {colId, ord} : null});
                savePage(0);
            },
            onFilterChange: appliedFilters => {
                const filters = {};
                appliedFilters.forEach(filter => filters[filter.column.field] = filter.value);
                save({filters: filters});
                savePage(0);
            },
            onPageChange: newPage => savePage(newPage),
            onRowsPerPageChange: size => {
                setPageSize(size);
                setTablePageSize(tableKey, size);
            }
        }
    };
}

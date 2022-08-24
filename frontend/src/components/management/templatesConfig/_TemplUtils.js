import {uuidV4} from "../../../utils/JsonHelper";
import {getCurrentDateTimeStr} from "../../../utils/DateTimeHelper";
import {getUsername} from "../../../utils/ConfigurationStorage";

export const getTemplatesArtefacts = (currentTemplate, allTemplates, artefactName) => {

    // get items from current template
    let availableItems = currentTemplate['configuration'][artefactName];

    // get items from base templates
    const templatesIds = currentTemplate['templatesIds'];
    if (templatesIds != null && templatesIds.length > 0) {
        for (const templId of templatesIds) {
            if (templId !== currentTemplate.id) {
                const filteredTemplates = allTemplates.filter(template => template.id === templId);
                if (filteredTemplates[0] != null) {
                    const templ = filteredTemplates[0];
                    if (templ['configuration'] != null)
                        availableItems = availableItems.concat(templ['configuration'][artefactName]);
                }
            }
        }
    }

    return availableItems
}

export const getTemplatesArtefactsEditable = (currentTemplate, allTemplates, artefactName) => {
    // get items from current template
    let availableItems = currentTemplate['configuration'][artefactName];

    // get items from base templates
    const templatesIds = currentTemplate['templatesIds'];
    if (templatesIds != null && templatesIds.length > 0) {
        for (const templId of templatesIds) {
            if (templId !== currentTemplate.id) {
                const filteredTemplates = allTemplates.filter(template => template.id === templId);
                if (filteredTemplates[0] != null) {
                    const templ = filteredTemplates[0];
                    if (templ['configuration'] != null) {
                        const baseArtefacts = templ['configuration'][artefactName];
                        if (baseArtefacts != null && baseArtefacts.length > 0) {
                            let enrichedArtefacts = [];
                            for (let i=0, max=baseArtefacts.length; i < max; i++) {
                                const artefact = baseArtefacts[i];
                                const enrichedArtefact = {...artefact, editable: false};
                                delete enrichedArtefact['tableData'];
                                enrichedArtefacts.push(enrichedArtefact);
                            }
                            availableItems = availableItems.concat(enrichedArtefacts);
                        }
                    }
                }
            }
        }
    }

    return availableItems;
}

export const templateArtefactCreationEnrichment = (data) => {
    data.id = uuidV4();
    const datetime = getCurrentDateTimeStr();
    const username = getUsername();
    data.createdAt = datetime;
    data.createdBy = username;
    data.modifiedAt = datetime;
    data.modifiedBy = username;
}

export const templateArtefactModifyingEnrichment = (data) => {
    data.modifiedAt = getCurrentDateTimeStr();
    data.modifiedBy = getUsername();
}

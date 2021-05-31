const CLASS_DEFAULT = 'none';

export const getClassesLookup = (classes) => {
    let lookup = {};
    lookup[CLASS_DEFAULT] = '- none -';
    if (classes != null)
        for (let i = 0; i < classes.length; i++)
            lookup[(classes[i]['id']).toString()] = classes[i]['name'];

    return lookup;
}
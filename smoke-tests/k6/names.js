// curl https://raw.githubusercontent.com/aruljohn/popular-baby-names/master/2017/boy_names_2017.json | jq '.names | .[]' | shuf | head -100 | sed -e "s/$/,/"
const firstNames = [
    "Finley", "Jade", "Kylee", "Kensley", "Kira", "Brenna", "Braelyn", "Marley", "Phoenix",
];

// $ curl https://raw.githubusercontent.com/rossgoodwin/american-names/master/surnames.json | jq | shuf | tail -25
const surnames = [
    "Coomes", "Kasputis", "Eing", "Budro", "Paszkiewicz", "Reichwald", "Mennona", "Esplin"
];

// source https://github.com/baliw/words/blob/master/adjectives.json
const adj = [
    "Ablaze", "Abrupt", "Accomplished", "Active", "Adored"
];

function randItem(list){
    return list[rand(list.length)];
}

function rand(max){
    return Math.floor(Math.random() * Math.floor(max))
}

function randomOwner(){
    const firstName = randItem(firstNames);
    const lastName = randItem(surnames);
    return {
     "firstName": firstName,
     "lastName": lastName,
     "address": `${rand(10000)} ${randItem(adj)} Ln.`,
     "city": "Anytown",
     "telephone": "8005551212",
     "pets": []
   };
}


export default {
    randomOwner
};

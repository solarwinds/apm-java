/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

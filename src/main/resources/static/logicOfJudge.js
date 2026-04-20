const questions = [
   {
     id: 1,
     title: "Two Sum",
     difficulty: "Easy",
     description: "Given an array of integers return indices of two numbers that add up to target.",
     examples: [
       { input: "nums = [2,7,11,15], target = 9", output: "[0,1]" }
     ],
     constraints: ["2 ≤ nums.length ≤ 10⁴"]
   },
   {
     id: 2,
     title: "Reverse String",
     difficulty: "Easy",
     description: "Given a string, return it reversed.",
     examples: [
       { input: "hello", output: "olleh" }
     ],
     constraints: ["1 ≤ s.length ≤ 10⁵"]
   }
 ];
 let currentIndex=0;
function renderQuestion(index)
{
	const q = questions[index];
	document.getElementById("question-pannel").innerHTML = `
	    <h3>${q.id}. ${q.title}</h3>
	    <span>${q.difficulty}</span>
	    <p>${q.description}</p>
	    <p><strong>Example:</strong><br>
	    Input: ${q.examples[0].input}<br>
	    Output: ${q.examples[0].output}</p>
	    <p><strong>Constraints:</strong> ${q.constraints[0]}</p>
	`;
}
function changeQuestion(direction)
{
	if(currentIndex<0)currentIndex=0;
	if(currentIndex>questions.length-1) currentIndex = questions.length-1;
	currentIndex = currentIndex + direction;
	renderQuestion(currentIndex);
}
async function submitCode() {
  const code = document.getElementById("codeInput").value?.trim();
  const input = document.getElementById("inputBox").value?.trim();
  const outputEl = document.getElementById("Output");
  const outputSection = document.getElementById("outputSection");
     
  if (!code) {
    outputEl.innerText = "⚠️ Please enter some code before running.";
    outputSection.style.display = "block";
    return;
  }

  outputEl.innerText = "⏳ Running...";
  outputSection.style.display = "block";

  try {
    const response = await fetch("http://localhost:8080/api/run", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code, input }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`Server error ${response.status}: ${errorText}`);
    }

    const result = await response.text();
    outputEl.innerText = result || "(no output)";

  } catch (err) {
    if (err.name === "TypeError" && err.message.includes("fetch")) {
      outputEl.innerText = "❌ Could not connect to server. Is it running on port 8080?";
    } else {
      outputEl.innerText = `❌ Error: ${err.message}`;
    }
  } finally {
    outputSection.scrollIntoView({ behavior: "smooth" });
  }
}
renderQuestion(0);
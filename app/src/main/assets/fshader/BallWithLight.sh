precision mediump float;
varying vec4 vDiffuse;//Receive scattered light components coming from the vertex shader
void main(){
   vec4 finalColor=vec4(1.0);
   //Give this element a meta color value
   gl_FragColor=finalColor*vDiffuse+finalColor*vec4(0.15,0.15,0.15,1.0);
}